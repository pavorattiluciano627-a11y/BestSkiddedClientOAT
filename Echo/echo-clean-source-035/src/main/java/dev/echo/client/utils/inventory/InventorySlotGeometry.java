package dev.echo.client.utils.inventory;

import net.minecraft.client.util.Window;
import net.minecraft.screen.slot.Slot;

/** Converts a screen-handler slot's logical center to window coordinates. */
public final class InventorySlotGeometry {
    private InventorySlotGeometry() {
    }

    public static double[] centerInWindow(Slot slot, int screenLeft, int screenTop, Window window) {
        // Preserve integer addition before conversion, including its overflow behavior.
        double centerX = screenLeft + slot.x + 8.0;
        double centerY = screenTop + slot.y + 8.0;
        double windowX = centerX * window.getWidth() / window.getScaledWidth();
        double windowY = centerY * window.getHeight() / window.getScaledHeight();
        return new double[] {windowX, windowY};
    }
}
