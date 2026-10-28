package dev.echo.client.utils.inventory;

import com.mojang.blaze3d.platform.Window;
import dev.echo.client.MinecraftAccess;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;

/**
 * Structured candidate for the inventory geometry family.
 * Analysis-only: promotion requires the dispatch and branch invariants in needs.json.
 */
public final class InventoryScreenSlots implements MinecraftAccess {
    public record ScreenLayout(AbstractContainerMenu handler, int backgroundWidth, int backgroundHeight) {
    }

    public record CursorTarget(int slotIndex, double windowX, double windowY, double distance) {
    }

    public InventoryScreenSlots() {
    }

    public static ScreenLayout layoutOf(Object screen) {
        if (screen instanceof InventoryScreen inventory) {
            return new ScreenLayout(inventory.getMenu(), 176, 166);
        }
        if (screen instanceof CreativeModeInventoryScreen creative) {
            return new ScreenLayout(creative.getMenu(), 195, 136);
        }
        return null;
    }

    public static boolean isPlayerInventoryScreen(Object screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
    }

    public static double[] centerInWindow(Slot slot, int left, int top, Window window) {
        return InventorySlotGeometry.centerInWindow(slot, left, top, window);
    }

    public static int[] screenOrigin(ScreenLayout layout, Window window) {
        int left = (window.getGuiScaledWidth() - layout.backgroundWidth()) / 2;
        int top = (window.getGuiScaledHeight() - layout.backgroundHeight()) / 2;
        return new int[] {left, top};
    }

    public static double farthestDistance(List<Slot> slots, boolean playerInventoryOnly,
            int left, int top, Window window, double cursorX, double cursorY) {
        return farthestDistance(slots, playerInventoryOnly, left, top, window, cursorX, cursorY, null);
    }

    public static double farthestDistance(List<Slot> slots, boolean playerInventoryOnly,
            int left, int top, Window window, double cursorX, double cursorY,
            Predicate<Slot> allowedSlot) {
        double farthest = 0.0;
        for (Slot slot : slots) {
            if (!isEligibleSlot(slot, playerInventoryOnly)) {
                continue;
            }
            if (allowedSlot != null && !allowedSlot.test(slot)) {
                continue;
            }
            double[] center = centerInWindow(slot, left, top, window);
            double distance = Math.hypot(center[0] - cursorX, center[1] - cursorY);
            if (distance > farthest) {
                farthest = distance;
            }
        }
        return farthest;
    }

    public static CursorTarget nearestItem(List<Slot> slots, Item item, boolean playerInventoryOnly,
            int left, int top, Window window, double cursorX, double cursorY) {
        return nearestItem(slots, item, playerInventoryOnly, left, top, window, cursorX, cursorY, null);
    }

    public static CursorTarget nearestItem(List<Slot> slots, Item item, boolean playerInventoryOnly,
            int left, int top, Window window, double cursorX, double cursorY,
            Predicate<Slot> allowedSlot) {
        double nearestDistance = Double.MAX_VALUE;
        CursorTarget nearest = null;
        for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
            Slot slot = slots.get(slotIndex);
            if (!isEligibleSlot(slot, playerInventoryOnly)) {
                continue;
            }
            if (allowedSlot != null && !allowedSlot.test(slot)) {
                continue;
            }
            if (!slot.getItem().is(item)) {
                continue;
            }
            double[] center = centerInWindow(slot, left, top, window);
            double distance = Math.hypot(center[0] - cursorX, center[1] - cursorY);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = new CursorTarget(slotIndex, center[0], center[1], distance);
            }
        }
        return nearest;
    }

    public static long cursorTravelDuration(double distance, double maximumDistance,
            float minimumDuration, float maximumDuration) {
        if (maximumDistance <= 0.0) {
            return Math.max(1L, (long) Math.round(minimumDuration));
        }
        double fraction = Math.max(0.0, Math.min(1.0, distance / maximumDistance));
        // The subtraction is performed as float before conversion to double.
        double duration = minimumDuration + fraction * (double) (maximumDuration - minimumDuration);
        return Math.max(1L, Math.round(duration));
    }

    private static boolean isEligibleSlot(Slot slot, boolean playerInventoryOnly) {
        if (playerInventoryOnly && !(slot.container instanceof Inventory)) {
            return false;
        }
        return !(slot.container instanceof Inventory) || slot.getContainerSlot() != 40;
    }
}
