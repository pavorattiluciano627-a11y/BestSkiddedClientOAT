package dev.echo.client.features.impl.utility;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.inventory.ContainerInput;
import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.events.input.MouseMovementEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.utils.RandomMath;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Replenishes low hotbar stacks and reproduces the original inventory-cursor animation. */
public class ReplenishFeature extends Feature {
    private static final String TIMING_SECTION = "Timing";
    private static FloatSetting minPercent;
    private static IntSetting cursorSpeed;
    private boolean movingCursor;
    private long cursorMoveStartMillis;
    private long cursorMoveDurationMillis;
    private double cursorStartX;
    private double cursorStartY;
    private double cursorTargetX;
    private double cursorTargetY;
    private int pendingInventorySlot;

    public ReplenishFeature() {
        super(new FeatureInfo(CatalogText.literal("Replenish"),
            CatalogText.literal("Automatically replenishes items"), Category.UTILITY));
    }

    @Override
    public void postInitialize() {
        movingCursor = false;
        cursorMoveStartMillis = 0L;
        cursorMoveDurationMillis = 200L;
        cursorStartX = 0.0;
        cursorStartY = 0.0;
        cursorTargetX = 0.0;
        cursorTargetY = 0.0;
        pendingInventorySlot = -1;
        cursorSpeed.tooltip(TIMING_SECTION);
    }

    @Subscribe
    private void onHandleInput(HandleInputEvent.Pre event) {
        if (isOutsideWorld() || !(MinecraftRenderState.getScreen() instanceof InventoryScreen)) {
            return;
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (stack.isEmpty() || !stack.isStackable()) {
                continue;
            }
            float percent = (float) stack.getCount() / (float) stack.getMaxStackSize() * 100.0f;
            if (stack.getCount() == 1 || percent <= Math.max(minPercent.getValue(), 5.0f)) {
                replenish(stack);
            }
        }
    }

    private void replenish(ItemStack hotbarStack) {
        for (int slot = 9; slot < 36; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (stack.isEmpty() || !sameReplenishmentItem(hotbarStack, stack)) {
                continue;
            }
            int inventoryIndex = inventoryIndexForSlot(slot);
            moveCursorToSlot(slot);
            quickMove(inventoryIndex);
            return;
        }
    }

    private boolean sameReplenishmentItem(ItemStack first, ItemStack second) {
        if (!first.getItem().equals(second.getItem())) {
            return false;
        }
        if (first.getItem() instanceof BlockItem firstBlock && second.getItem() instanceof BlockItem secondBlock) {
            return firstBlock.getBlock() == secondBlock.getBlock();
        }
        return true;
    }

    private int inventoryIndexForSlot(int slot) {
        return client.player.containerMenu.slots.get(slot).getContainerSlot();
    }

    private void quickMove(int slot) {
        int syncId = client.player.containerMenu.containerId;
        Player player = client.player;
        if (player == null || client.gameMode == null) {
            return;
        }
        client.gameMode.handleContainerInput(syncId, slot, 0, ContainerInput.QUICK_MOVE, player);
    }

    private void moveCursorToSlot(int slotIndex) {
        if (MinecraftRenderState.getScreen() == null) {
            return;
        }
        var handler = ((InventoryScreen) MinecraftRenderState.getScreen()).getMenu();
        var window = client.getWindow();
        double mouseX = client.mouseHandler.xpos();
        double mouseY = client.mouseHandler.ypos();
        int left = (window.getGuiScaledWidth() - 176) / 2;
        int top = (window.getGuiScaledHeight() - 166) / 2;
        var slot = handler.slots.get(slotIndex);
        double targetX = (double) (left + slot.x) + 8.0;
        double targetY = (double) (top + slot.y) + 8.0;
        cursorStartX = mouseX;
        cursorStartY = mouseY;
        cursorMoveDurationMillis = 200L - (long) cursorSpeed.getValue() * 2L;
        cursorTargetX = targetX;
        cursorTargetY = targetY;
        cursorMoveStartMillis = System.currentTimeMillis();
        movingCursor = true;
        pendingInventorySlot = slotIndex;
    }

    @Subscribe
    private void onMouseMovement(MouseMovementEvent.Post event) {
        if (!movingCursor) {
            return;
        }
        if (client == null || MinecraftRenderState.getScreen() == null) {
            movingCursor = false;
            return;
        }
        if (!(MinecraftRenderState.getScreen() instanceof InventoryScreen)) {
            movingCursor = false;
            return;
        }
        long elapsed = System.currentTimeMillis() - cursorMoveStartMillis;
        double progress = Math.max(0.0, Math.min(1.0, (double) elapsed / (double) cursorMoveDurationMillis));
        double eased = RandomMath.smoothStep(progress, 0.0, 1.0);
        double x = cursorStartX + (cursorTargetX - cursorStartX) * eased;
        double y = cursorStartY + (cursorTargetY - cursorStartY) * eased;
        var window = client.getWindow();
        double windowX = x * (double) window.getScreenWidth() / (double) window.getGuiScaledWidth();
        double windowY = y * (double) window.getScreenHeight() / (double) window.getGuiScaledHeight();
        InputConstants.grabOrReleaseMouse(window, GLFW.GLFW_CURSOR_NORMAL, windowX, windowY);
        if (progress >= 1.0) {
            movingCursor = false;
            if (pendingInventorySlot >= 0) {
                quickMove(pendingInventorySlot);
                pendingInventorySlot = -1;
            }
        }
    }

    public static void appendBootstrapSettings(ReplenishFeature feature) {
        CatalogSettings.append(feature, minPercent);
        CatalogSettings.append(feature, cursorSpeed);
    }

    public static void assignBootstrapSettings(ReplenishFeature feature, FloatSetting preparedMinPercent,
            IntSetting preparedCursorSpeed) {
        minPercent = preparedMinPercent;
        cursorSpeed = preparedCursorSpeed;
    }
}
