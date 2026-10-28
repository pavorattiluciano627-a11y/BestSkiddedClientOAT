package dev.echo.client.hud;

/** Fixed sizing and alignment rules extracted from the original HUD renderer. */
public final class HudLayout {
    private HudLayout() {
    }

    /** Measures the target's name/health content area with its original minimum. */
    public static float targetContentWidth(float nameWidth, float healthWidth) {
        float width = Math.max(0.0f, nameWidth) + 14.0f + Math.max(0.0f, healthWidth);
        return (float) Math.ceil(Math.max(96.0f, width));
    }

    /** Adds portrait and outer padding to the target content area. */
    public static float targetPanelWidth(float contentWidth) {
        return 62.0f + Math.max(96.0f, contentWidth) + 10.0f;
    }

    /** Measures the effects list including its header. */
    public static float effectsPanelHeight(int rows) {
        return 54.0f + Math.max(0, rows) * 23.0f;
    }

    /** Aligns a nonnegative width around a center coordinate. */
    public static float centeredLeft(float center, float width) {
        return center - Math.max(0.0f, width) * 0.5f;
    }

    /** Aligns a nonnegative width to a right edge. */
    public static float rightAlignedLeft(float right, float width) {
        return right - Math.max(0.0f, width);
    }

    /** Rounds a width upward to the HUD's five-unit size grid. */
    public static float roundWidth(float width) {
        return (float) Math.ceil(Math.max(0.0f, width) / 5.0f) * 5.0f;
    }

    /** Returns the hotbar item Y position. */
    public static float hotbarItemY(float top) {
        return top + 14.0f;
    }

    /** Returns the armor item Y position. */
    public static float armorItemY(float top) {
        return top + 14.0f;
    }

    /** Returns the equipment item Y position. */
    public static float equipmentItemY(float top) {
        return top + 14.0f;
    }

    /** Returns the equipment label baseline. */
    public static float equipmentLabelY(float top) {
        return top + 13.5f;
    }

    /** Clamps an index to the original slot range, including an empty range. */
    public static int slotIndex(int index, int slotCount) {
        return Math.max(0, Math.min(Math.max(0, slotCount - 1), index));
    }

    /** Restricts a render fraction while preserving floating-point NaN behavior. */
    public static float clampFraction(float fraction) {
        return Math.max(0.0f, Math.min(1.0f, fraction));
    }
}
