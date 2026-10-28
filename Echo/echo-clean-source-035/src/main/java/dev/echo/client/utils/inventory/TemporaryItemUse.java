package dev.echo.client.utils.inventory;

import static dev.echo.client.MinecraftAccess.CLIENT;
import dev.echo.client.events.EventPriority;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.events.input.HotbarKeyEvent;
import dev.echo.client.events.input.SwapHandsKeyEvent;
import dev.echo.client.events.interaction.AttackAttemptEvent;
import dev.echo.client.events.interaction.HandInteractEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.network.PacketSendEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.utils.items.InventoryItems;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Feature-owned select/swap/reselect state machine, driven by the original input and packet events. */
public final class TemporaryItemUse {
    private static final int SWAP_DELAY_TICKS = 2;
    private static final int TIMEOUT_TICKS = 20;
    private final Feature owner;
    private final Predicate<Item> itemPredicate;
    private final Consumer<Item> transferCallback;
    private TemporaryItemUseState state = TemporaryItemUseState.IDLE;
    private long nextActionTick = -1L;
    private int targetSlot = -1;
    private int previousSlot = -1;
    private Item requestedItem;
    private boolean swapKeyActive;

    public TemporaryItemUse(Feature owner, Predicate<Item> itemPredicate, Consumer<Item> transferCallback) {
        this.owner = owner;
        this.itemPredicate = itemPredicate;
        this.transferCallback = transferCallback;
    }

    public boolean isBusy() { return state != TemporaryItemUseState.IDLE; }
    public boolean isSwapKeyActive() { return swapKeyActive; }

    public void beginSwap(int slot, Item item) {
        requestedItem = item;
        targetSlot = slot;
        previousSlot = selectedSlot();
        enterState(slot == previousSlot ? TemporaryItemUseState.SWAP : TemporaryItemUseState.SELECT, 0);
    }

    public void cancelAndRestore() {
        if (isBusy() && CLIENT.player != null && targetSlot != previousSlot && selectedSlot() == targetSlot) {
            if (ownsTopSelection()) {
                DeferredActionQueue.select(owner, previousSlot, -1, false);
            } else if (!DeferredActionQueue.contains(owner)) {
                InventoryItems.selectHotbarSlot(previousSlot);
            }
        }
        resetState();
    }

    public void reset() { resetState(); }

    @Subscribe
    private void onClientTick(ClientTickEvent event) {
        if (!isBusy() || CLIENT.player == null) { return; }
        boolean timedOut = (long) CLIENT.player.tickCount - nextActionTick > TIMEOUT_TICKS;
        boolean changedSlot = state != TemporaryItemUseState.SELECT && selectedSlot() != targetSlot;
        if (timedOut || changedSlot) { cancelAndRestore(); }
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (!canHandleInput()) { return; }
        if (state == TemporaryItemUseState.SELECT && event.slot == targetSlot) {
            selectTarget(event);
        } else if (state == TemporaryItemUseState.RESELECT && event.slot == previousSlot) {
            restoreSelection(event);
        }
    }

    @Subscribe
    private void onSwapHandsKey(SwapHandsKeyEvent event) {
        if (state != TemporaryItemUseState.SWAP || !canHandleInput()) { return; }
        if (!canSwapItem()) {
            scheduleReselect();
            return;
        }
        swapKeyActive = true;
        event.setClickCount(1);
    }

    @Subscribe
    private void onPacketSend(PacketSendEvent event) {
        if (!swapKeyActive || event.cancelled) { return; }
        if (!(event.getPacket() instanceof ServerboundPlayerActionPacket packet)
                || packet.getAction() != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) { return; }
        swapKeyActive = false;
        swapLocalHands();
        transferCallback.accept(requestedItem);
        scheduleReselect();
    }

    @Subscribe
    private void onInputComplete(HandleInputEvent.Post event) {
        if (!swapKeyActive) { return; }
        swapKeyActive = false;
        scheduleReselect();
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onAttackAttempt(AttackAttemptEvent event) {
        if (isBusy()) { event.cancel(); }
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onHandInteract(HandInteractEvent.Pre event) {
        if (isBusy()) { event.cancel(); }
    }

    private void selectTarget(HotbarKeyEvent event) {
        if (!DeferredActionQueue.select(owner, targetSlot, -1, false)) { return; }
        event.addClick();
        enterState(TemporaryItemUseState.SWAP, SWAP_DELAY_TICKS);
    }

    private void restoreSelection(HotbarKeyEvent event) {
        boolean buriedOwner = DeferredActionQueue.contains(owner) && !ownsTopSelection();
        if (selectedSlot() == targetSlot && !buriedOwner) {
            if (ownsTopSelection() && !DeferredActionQueue.select(owner, previousSlot, -1, false)) { return; }
            event.addClick();
        }
        // The original final dynamic site invokes prepareAccount(J)I and discards
        // its return value. It is not a reset; the next client tick handles cleanup.
        selectedSlot();
    }

    private boolean canSwapItem() {
        return MinecraftRenderState.getScreen() == null && selectedSlot() == targetSlot
                && CLIENT.player.getMainHandItem().is(requestedItem) && itemPredicate.test(requestedItem);
    }

    private void scheduleReselect() {
        if (targetSlot == previousSlot || selectedSlot() != targetSlot) {
            resetState();
            return;
        }
        enterState(TemporaryItemUseState.RESELECT, 1);
    }

    private void swapLocalHands() {
        ItemStack offhand = CLIENT.player.getItemInHand(InteractionHand.OFF_HAND);
        CLIENT.player.setItemInHand(InteractionHand.OFF_HAND, CLIENT.player.getItemInHand(InteractionHand.MAIN_HAND));
        CLIENT.player.setItemInHand(InteractionHand.MAIN_HAND, offhand);
    }

    private boolean canHandleInput() {
        return isBusy() && CLIENT.player != null && (long) CLIENT.player.tickCount - nextActionTick >= 0L;
    }

    private boolean ownsTopSelection() { return DeferredActionQueue.selectedSlot(owner) != -1; }
    private int selectedSlot() { return CLIENT.player.getInventory().getSelectedSlot(); }

    private void enterState(TemporaryItemUseState next, int delayTicks) {
        state = next;
        nextActionTick = (long) (CLIENT.player.tickCount + delayTicks);
    }

    private void resetState() {
        DeferredActionQueue.drop(owner);
        state = TemporaryItemUseState.IDLE;
        nextActionTick = -1L;
        targetSlot = -1;
        previousSlot = -1;
        requestedItem = null;
        swapKeyActive = false;
    }
}
