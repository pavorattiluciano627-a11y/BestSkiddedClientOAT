package dev.echo.client.utils.inventory;

import static dev.echo.client.MinecraftAccess.CLIENT;

import dev.echo.client.events.EventPriority;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.events.interaction.AttackAttemptEvent;
import dev.echo.client.events.interaction.HandInteractEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.render.HudRenderEvent;
import dev.echo.client.mixin.HandledScreenAccessor;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.Setting;
import dev.echo.client.utils.ActionTimer;
import dev.echo.client.utils.RandomMath;
import dev.echo.client.utils.strings.Concat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

/**
 * Timed inventory transfer, silent-open state and cursor movement. Direct-operation
 * and original guard/caller qualifications remain recorded in the provider receipt.
 */
public final class InventoryActionScheduler {
    private static final CharSequence OPEN_INVENTORY = label("Open Inventory");
    private static final CharSequence SILENT_OPEN = label("Silent Open");
    private static final CharSequence DONT_OPEN = label("Dont Open");
    private static final CharSequence AUTOMATIC = label("Automatic");
    private static final CharSequence HOVER = label("Hover");
    private static final CharSequence CURSOR_SIMULATION = label("Cursor Simulation");
    private static final float MIN_CURSOR_DURATION = 50.0F;
    private static final float MAX_CURSOR_DURATION = 250.0F;
    private final ChoiceSetting openMode;
    private final ChoiceSetting selectionMode = new ChoiceSetting(label("Selection"), AUTOMATIC, AUTOMATIC, HOVER, CURSOR_SIMULATION);
    private final BooleanSetting autoClose = new BooleanSetting(label("Auto Close"), true);
    private final FloatRangeSetting closeDelay = new FloatRangeSetting(label("Close Delay"), 50F, 150F, 0F, 500F, 25F, label(" ms"));
    private final FloatRangeSetting silentMoveDelay = new FloatRangeSetting(label("Silent Move Delay"), 100F, 300F, 0F, 500F, 25F, label(" ms"));
    private final BooleanSetting randomSlot = new BooleanSetting(label("Random Slot"), false);
    private final FloatRangeSetting transferDelay = new FloatRangeSetting(label("Delay"), 50F, 150F, 0F, 500F, 25F, label(" ms"));
    private static boolean transferring;
    private final Supplier<Item> itemSupplier;
    private final IntSupplier targetSlotSupplier;
    private final BooleanSupplier enabledCondition;
    private final boolean allowHotbarSource;
    private IntPredicate sourceSlotPredicate = slot -> true;
    private BiConsumer<Integer, Item> transferCallback = (slot, item) -> { };
    private final ActionTimer transferTimer = new ActionTimer();
    private final ActionTimer silentTimer = new ActionTimer();
    private final ActionTimer afterOpenTimer = new ActionTimer();
    private final ActionTimer closeTimer = new ActionTimer();
    private final Random random = new Random();
    private long nextTransferDelay;
    private long stateDelay;
    private long nextCloseDelay;
    private boolean autoClosePending;
    private boolean openedForHoverTarget;
    private boolean initialDelayArmed;
    private int lastTarget = -1;
    private InventoryActionState state = InventoryActionState.IDLE;
    private int pendingSource = -1;
    private int pendingTarget = -1;
    private boolean cursorMoving;
    private boolean cursorClickPending;
    private long cursorStartedAt;
    private long cursorDuration;
    private double cursorStartX;
    private double cursorStartY;
    private double cursorEndX;
    private double cursorEndY;
    private int cursorSource = -1;
    private int cursorTarget = -1;

    public InventoryActionScheduler(CharSequence label, Item item, IntSupplier targetSlot,
            BooleanSupplier enabled, BooleanSupplier visible) {
        this(label, item, targetSlot, enabled, visible, false);
    }

    public InventoryActionScheduler(CharSequence label, Item item, IntSupplier targetSlot,
            BooleanSupplier enabled, BooleanSupplier visible, boolean allowHotbarSource) {
        this(label, () -> item, targetSlot, enabled, visible, allowHotbarSource);
    }

    public InventoryActionScheduler(CharSequence label, Supplier<Item> item, IntSupplier targetSlot,
            BooleanSupplier enabled, BooleanSupplier visible, boolean allowHotbarSource) {
        itemSupplier = item;
        targetSlotSupplier = targetSlot;
        enabledCondition = enabled;
        this.allowHotbarSource = allowHotbarSource;
        openMode = new ChoiceSetting(label, OPEN_INVENTORY, OPEN_INVENTORY, SILENT_OPEN, DONT_OPEN);
        openMode.setVisibilityPredicate(ignored -> visible.getAsBoolean());
        selectionMode.setVisibilityPredicate(ignored -> visible.getAsBoolean() && usesVisibleSelection());
        autoClose.setVisibilityPredicate(ignored -> visible.getAsBoolean());
        closeDelay.setVisibilityPredicate(ignored -> visible.getAsBoolean() && autoClose.getValue());
        silentMoveDelay.setVisibilityPredicate(ignored -> visible.getAsBoolean() && openMode.isSelected(SILENT_OPEN));
        randomSlot.setVisibilityPredicate(ignored -> visible.getAsBoolean() && (openMode.isSelected(SILENT_OPEN) || isAutomaticSelection()));
        transferDelay.setVisibilityPredicate(ignored -> visible.getAsBoolean());
    }

    private static Concat label(String text) {
        return Concat.strings(text.codePoints().mapToObj(value -> new String(Character.toChars(value))).toArray(String[]::new));
    }

    public static boolean isTransferring() { return transferring; }
    public InventoryActionScheduler withSlotPredicate(IntPredicate predicate) { sourceSlotPredicate = predicate; return this; }
    public InventoryActionScheduler onTransfer(BiConsumer<Integer, Item> callback) { transferCallback = callback; return this; }
    public List<Setting> getSettings() { return List.of(openMode, selectionMode, autoClose, closeDelay, silentMoveDelay, randomSlot, transferDelay); }
    public boolean isActive() { return state != InventoryActionState.IDLE; }

    public void reset() {
        if (CLIENT.player != null) { abortSilent(); }
        autoClosePending = false;
        stateDelay = 0L;
        transferTimer.reset();
        silentTimer.reset();
        afterOpenTimer.reset();
        closeTimer.reset();
        nextTransferDelay = sampleTransferDelay();
        nextCloseDelay = sampleCloseDelay();
        openedForHoverTarget = false;
        initialDelayArmed = false;
        lastTarget = -1;
        resetSilentState();
        cancelCursor();
    }

    @Subscribe
    private void onClientTick(ClientTickEvent event) {
        if (missingWorld() || CLIENT.interactionManager == null) { return; }
        if (!enabledCondition.getAsBoolean()) {
            abortSilent();
            if (cursorBusy()) { cancelCursor(); }
            return;
        }
        if (hasBlockingScreen()) {
            if (cursorBusy()) { cancelCursor(); }
            return;
        }
        if (hasCursorStack()) {
            abortSilent();
            if (cursorBusy()) { cancelCursor(); }
            autoClosePending = false;
            return;
        }
        if (!openMode.isSelected(SILENT_OPEN)) { abortSilent(); }
        if (!isCursorSelection() && cursorBusy()) { cancelCursor(); }
        if (openMode.isSelected(SILENT_OPEN) && isActive()) { advanceSilent(); return; }
        if (cursorClickPending) { completeCursorTransfer(); return; }
        updateAutoClose();
        updateInitialDelay();
        if (cursorBusy()) { return; }
        int target = targetSlotSupplier.getAsInt();
        if (target != lastTarget) { openedForHoverTarget = false; lastTarget = target; }
        if (target == -1) { return; }
        processTarget(target);
    }

    @Subscribe
    private void onHudRender(HudRenderEvent event) {
        if (cursorMoving) { updateCursor(); }
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onPlayerInput(PlayerInputEvent event) {
        if (!isActive()) { return; }
        event.forward = false; event.backward = false; event.left = false; event.right = false;
        event.jump = false; event.sneak = false; event.sprint = false; event.allowMovementInFreecam = true;
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onHandInteract(HandInteractEvent.Pre event) { if (isActive()) { event.cancel(); } }
    @Subscribe(priority = EventPriority.HIGH)
    private void onAttackAttempt(AttackAttemptEvent event) { if (isActive()) { event.cancel(); } }

    private void processTarget(int target) {
        if (openMode.isSelected(OPEN_INVENTORY) && !isInventoryScreen()) {
            if (firstSource(target) == -1 || !armInitialDelay() || !transferDelayElapsed()) { return; }
            if (isHoverSelection() && openedForHoverTarget) { return; }
            ensureScreenOpen();
            disarmInitialDelay();
            if (isHoverSelection()) { openedForHoverTarget = true; }
            return;
        }
        int source = findSource(target);
        if (source == -1 || !transferDelayElapsed()) { return; }
        if (openMode.isSelected(SILENT_OPEN)) { queueSilent(source, target); return; }
        if (!ensureScreenOpen()) { return; }
        if (isCursorSelection()) { startCursor(source, target); return; }
        transfer(source, target);
    }

    private boolean transferDelayElapsed() { return transferTimer.hasElapsedMillis((double) nextTransferDelay); }
    private boolean armInitialDelay() {
        if (initialDelayArmed) { return true; }
        initialDelayArmed = true;
        transferTimer.reset();
        nextTransferDelay = sampleTransferDelay();
        return false;
    }
    private void updateInitialDelay() {
        if (initialDelayArmed && !shouldKeepInitialDelay()) { disarmInitialDelay(); }
    }
    private boolean shouldKeepInitialDelay() {
        if (!openMode.isSelected(OPEN_INVENTORY) || isInventoryScreen()) { return false; }
        int target = targetSlotSupplier.getAsInt();
        return target != -1 && firstSource(target) != -1;
    }
    private void disarmInitialDelay() { initialDelayArmed = false; }

    private boolean ensureScreenOpen() {
        if (openMode.isSelected(OPEN_INVENTORY)) {
            if (isInventoryScreen()) { return true; }
            MinecraftRenderState.setScreen(new InventoryScreen(CLIENT.player));
            return false;
        }
        if (openMode.isSelected(DONT_OPEN)) { return isInventoryScreen(); }
        return !openMode.isSelected(SILENT_OPEN);
    }

    private int findSource(int target) {
        if (isHoverSelection()) { return hoveredSource(target); }
        if (isCursorSelection()) { return nearestSourceIndex(target); }
        return randomSlot.getValue() ? randomSource(target) : firstSource(target);
    }
    private int hoveredSource(int target) {
        if (!isInventoryScreen()) { return -1; }
        Screen screen = MinecraftRenderState.getScreen();
        if (!(screen instanceof HandledScreenAccessor accessor)) { return -1; }
        Slot slot = accessor.getFocusedSlot();
        if (slot == null || !slot.getStack().isOf(itemSupplier.get()) || !allowedScreenSlot(slot, target)) { return -1; }
        return CLIENT.player.currentScreenHandler.slots.indexOf(slot);
    }
    private int nearestSourceIndex(int target) {
        InventoryScreenSlots.CursorTarget nearest = nearestSource(target);
        return nearest == null ? -1 : nearest.slotIndex();
    }
    private InventoryScreenSlots.CursorTarget nearestSource(int target) {
        if (!isInventoryScreen()) { return null; }
        InventoryScreenSlots.ScreenLayout layout = InventoryScreenSlots.layoutOf(MinecraftRenderState.getScreen());
        if (layout == null) { return null; }
        Window window = CLIENT.getWindow();
        int[] origin = InventoryScreenSlots.screenOrigin(layout, window);
        double mouseX = CLIENT.mouse.getX(), mouseY = CLIENT.mouse.getY();
        boolean creative = MinecraftRenderState.getScreen() instanceof CreativeInventoryScreen;
        return InventoryScreenSlots.nearestItem(layout.handler().slots, itemSupplier.get(), creative,
                origin[0], origin[1], window, mouseX, mouseY, slot -> allowedScreenSlot(slot, target));
    }
    private boolean allowedScreenSlot(Slot slot, int target) {
        return !(slot.inventory instanceof PlayerInventory) || allowedInventorySlot(slot.getIndex(), target);
    }
    private boolean allowedInventorySlot(int slot, int target) {
        return slot >= (allowHotbarSource ? 0 : 9) && slot < 36 && slot != target && sourceSlotPredicate.test(slot);
    }
    private int firstSource(int target) {
        for (int screenSlot = 9; screenSlot < (allowHotbarSource ? 45 : 36); screenSlot++) {
            int inventorySlot = screenSlot >= 36 ? screenSlot - 36 : screenSlot;
            if (allowedInventorySlot(inventorySlot, target) && CLIENT.player.getInventory().getStack(inventorySlot).isOf(itemSupplier.get())) {
                return screenSlot;
            }
        }
        return -1;
    }
    private int randomSource(int target) {
        List<Integer> candidates = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            if (allowedInventorySlot(slot, target) && CLIENT.player.getInventory().getStack(slot).isOf(itemSupplier.get())) {
                candidates.add(screenSlot(slot));
            }
        }
        return candidates.isEmpty() ? -1 : candidates.get(random.nextInt(candidates.size()));
    }
    private boolean hasBlockingScreen() { return MinecraftRenderState.getScreen() != null && !isInventoryScreen(); }
    private boolean hasCursorStack() { return !CLIENT.player.currentScreenHandler.getCursorStack().isEmpty(); }
    private boolean isInventoryScreen() {
        return MinecraftRenderState.getScreen() instanceof InventoryScreen
                || MinecraftRenderState.getScreen() instanceof CreativeInventoryScreen;
    }
    private boolean missingWorld() { return CLIENT.player == null || CLIENT.world == null; }

    private void transfer(int source, int target) { transfer(source, target, true); }
    private void transfer(int source, int target, boolean scheduleClose) {
        Item item = itemSupplier.get();
        transferring = true;
        try {
            int syncId = CLIENT.player.currentScreenHandler.syncId;
            if (CLIENT.player != null && CLIENT.interactionManager != null) {
                CLIENT.interactionManager.clickSlot(syncId, source, target, SlotActionType.SWAP, CLIENT.player);
            }
        } finally {
            transferring = false;
        }
        transferCallback.accept(target, item);
        transferTimer.reset();
        nextTransferDelay = sampleTransferDelay();
        openedForHoverTarget = false;
        if (scheduleClose) { scheduleAutoClose(); }
    }

    private void startCursor(int source, int target) {
        if (cursorBusy() || !isInventoryScreen()) { return; }
        InventoryScreenSlots.ScreenLayout layout = InventoryScreenSlots.layoutOf(MinecraftRenderState.getScreen());
        if (layout == null || source < 0 || source >= layout.handler().slots.size()) { return; }
        Window window = CLIENT.getWindow();
        int[] origin = InventoryScreenSlots.screenOrigin(layout, window);
        double mouseX = CLIENT.mouse.getX(), mouseY = CLIENT.mouse.getY();
        boolean creative = MinecraftRenderState.getScreen() instanceof CreativeInventoryScreen;
        double maximum = InventoryScreenSlots.farthestDistance(layout.handler().slots, creative,
                origin[0], origin[1], window, mouseX, mouseY, slot -> allowedScreenSlot(slot, target));
        double[] center = InventoryScreenSlots.centerInWindow(layout.handler().slots.get(source), origin[0], origin[1], window);
        double distance = Math.hypot(center[0] - mouseX, center[1] - mouseY);
        cursorStartX = mouseX; cursorStartY = mouseY; cursorEndX = center[0]; cursorEndY = center[1];
        cursorDuration = InventoryScreenSlots.cursorTravelDuration(distance, maximum, MIN_CURSOR_DURATION, MAX_CURSOR_DURATION);
        cursorStartedAt = System.currentTimeMillis();
        cursorSource = source; cursorTarget = target;
        cursorMoving = true; cursorClickPending = false;
        CLIENT.execute(() -> CLIENT.mouse.unlockCursor());
    }
    private void updateCursor() {
        if (CLIENT.player == null || CLIENT.player.isDead() || !isCursorSelection() || !isInventoryScreen()) {
            cancelCursor(); return;
        }
        long elapsed = System.currentTimeMillis() - cursorStartedAt;
        double fraction = Math.max(0.0, Math.min(1.0, (double) elapsed / cursorDuration));
        double eased = RandomMath.smoothStep(fraction, 0.0, 1.0);
        double x = cursorStartX + (cursorEndX - cursorStartX) * eased;
        double y = cursorStartY + (cursorEndY - cursorStartY) * eased;
        InputUtil.setCursorParameters(CLIENT.getWindow(), GLFW.GLFW_CURSOR_NORMAL, x, y);
        if (fraction < 1.0) { return; }
        cursorMoving = false; cursorClickPending = true;
    }
    private void completeCursorTransfer() {
        InventoryScreenSlots.ScreenLayout layout = InventoryScreenSlots.layoutOf(MinecraftRenderState.getScreen());
        if (layout == null || CLIENT.interactionManager == null || cursorSource < 0 || cursorSource >= layout.handler().slots.size()
                || !targetNeedsItem(cursorTarget)) { cancelCursor(); return; }
        Slot source = layout.handler().slots.get(cursorSource);
        if (!source.getStack().isOf(itemSupplier.get())) { cancelCursor(); return; }
        transfer(cursorSource, cursorTarget);
        cancelCursor();
    }
    private boolean targetNeedsItem(int target) {
        if (target == 40) { return !CLIENT.player.getOffHandStack().isOf(itemSupplier.get()); }
        return target >= 0 && target <= 8 && !CLIENT.player.getInventory().getStack(target).isOf(itemSupplier.get());
    }
    private int screenSlot(int inventorySlot) { return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot; }
    private long sampleTransferDelay() { return Math.max(0L, (long) transferDelay.sampleGaussian()); }
    private long sampleSilentMoveDelay() { return Math.max(0L, (long) silentMoveDelay.sampleGaussian()); }
    private long sampleCloseDelay() { return Math.max(0L, (long) closeDelay.sampleGaussian()); }

    private void scheduleAutoClose() {
        if (!autoClose.getValue()) { return; }
        nextCloseDelay = sampleCloseDelay();
        if (nextCloseDelay <= 0L) { closeInventory(); return; }
        closeTimer.reset();
        autoClosePending = true;
    }
    private void updateAutoClose() {
        if (!autoClosePending) { return; }
        if (!autoClose.getValue()) { autoClosePending = false; return; }
        if (openMode.isSelected(OPEN_INVENTORY) && !isInventoryScreen()) { autoClosePending = false; return; }
        if (closeTimer.hasElapsedMillis((double) nextCloseDelay)) { closeInventory(); }
    }
    private void closeInventory() {
        if (CLIENT.player == null) { autoClosePending = false; return; }
        if (isInventoryScreen()) { CLIENT.player.closeHandledScreen(); }
        else { sendClosePacket(CLIENT.player.currentScreenHandler.syncId); }
        autoClosePending = false;
    }
    private static void sendClosePacket(int syncId) {
        if (CLIENT.getNetworkHandler() != null) { CLIENT.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(syncId)); }
    }
    private void queueSilent(int source, int target) {
        if (isActive()) { return; }
        pendingSource = source; pendingTarget = target;
        state = InventoryActionState.WAIT_BEFORE_OPEN;
        stateDelay = sampleSilentMoveDelay();
        silentTimer.reset();
    }
    private void advanceSilent() {
        if (state == InventoryActionState.WAIT_BEFORE_OPEN) {
            if (!silentTimer.hasElapsedMillis((double) stateDelay)) { return; }
            CLIENT.player.openRidingInventory();
            state = InventoryActionState.WAIT_AFTER_OPEN;
            afterOpenTimer.reset();
            return;
        }
        if (state == InventoryActionState.WAIT_AFTER_OPEN) {
            if (!afterOpenTimer.hasElapsedTicks(1)) { return; }
            if (!pendingTransferValid()) { abortSilent(); return; }
            transfer(pendingSource, pendingTarget, false);
            if (!autoClose.getValue()) { resetSilentState(); return; }
            state = InventoryActionState.WAIT_BEFORE_CLOSE;
            stateDelay = sampleSilentMoveDelay();
            silentTimer.reset();
            return;
        }
        if (state == InventoryActionState.WAIT_BEFORE_CLOSE) {
            if (!silentTimer.hasElapsedMillis((double) stateDelay)) { return; }
            sendClosePacket(CLIENT.player.currentScreenHandler.syncId);
            resetSilentState();
        }
    }
    private boolean pendingTransferValid() {
        if (pendingTarget != targetSlotSupplier.getAsInt() || !targetNeedsItem(pendingTarget)
                || pendingSource < 0 || pendingSource >= CLIENT.player.currentScreenHandler.slots.size()) { return false; }
        return CLIENT.player.currentScreenHandler.slots.get(pendingSource).getStack().isOf(itemSupplier.get());
    }
    private void abortSilent() {
        if (state == InventoryActionState.WAIT_AFTER_OPEN || state == InventoryActionState.WAIT_BEFORE_CLOSE) {
            sendClosePacket(CLIENT.player.currentScreenHandler.syncId);
        }
        resetSilentState();
    }
    private boolean usesVisibleSelection() { return openMode.isSelected(OPEN_INVENTORY) || openMode.isSelected(DONT_OPEN); }
    private boolean isAutomaticSelection() { return usesVisibleSelection() && selectionMode.isSelected(AUTOMATIC); }
    private boolean isHoverSelection() { return usesVisibleSelection() && selectionMode.isSelected(HOVER); }
    private boolean isCursorSelection() { return usesVisibleSelection() && selectionMode.isSelected(CURSOR_SIMULATION); }
    private boolean cursorBusy() { return cursorMoving || cursorClickPending; }
    private void resetSilentState() { state = InventoryActionState.IDLE; pendingSource = -1; pendingTarget = -1; stateDelay = 0L; }
    private void cancelCursor() {
        cursorMoving = false; cursorClickPending = false; cursorStartedAt = 0L; cursorDuration = 0L;
        cursorSource = -1; cursorTarget = -1;
    }
}
