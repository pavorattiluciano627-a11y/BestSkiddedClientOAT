package dev.echo.client.screens.dashboard;

import dev.echo.client.ui.UiRect;

/** Original dashboard measurements in its 1920 by 1080 logical coordinate space. */
public final class DashboardLayout {
    private DashboardLayout() {
    }

    /** Returns the original content height. */
    public static float contentHeight() {
        return 842.0f;
    }

    /** Returns the height remaining after the top and bottom content insets. */
    public static float availableContentHeight() {
        return contentHeight() - 20.0f - 140.0f;
    }

    /** Returns the module card rectangle at an index in the three-column grid. */
    public static UiRect moduleCard(int index) {
        return new UiRect(346.0f + index % 3 * 509.0f,
                102.0f + index / 3 * 74.0f, 493.0f, 63.0f);
    }

    /** Returns a script card's column position using Java's signed remainder. */
    public static float scriptCardX(int index) {
        return 354 + index % 3 * 509;
    }

    /** Returns the configuration card's more-menu button bounds. */
    public static UiRect configMenuButton(float cardX, float cardY) {
        return new UiRect(cardX + 455.0f, cardY + 17.0f, 22.0f, 22.0f);
    }

    /** Returns the lower edge of the original two-column option grid. */
    public static float optionGridBottom(float top, int count) {
        return top + 48.0f + Math.max(1, (count + 1) / 2) * 44;
    }

    /** Restricts a slider fraction to its usable interval, propagating NaN. */
    public static float clampFraction(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    /** Converts a slider value to its thumb center, accounting for thumb width. */
    public static float sliderThumbCenter(float left, float width, float thumbWidth, float value) {
        float halfThumb = thumbWidth * 0.5f;
        float fraction = Math.max(0.0f, Math.min(1.0f, value));
        return left + halfThumb + Math.max(0.0f, width - thumbWidth) * fraction;
    }

    /** Maps a mouse coordinate back to a slider fraction. */
    public static float sliderValue(float mouseX, float left, float width, float thumbWidth) {
        float travel = Math.max(1.0e-4f, width - thumbWidth);
        return Math.max(0.0f, Math.min(1.0f, (mouseX - left - thumbWidth * 0.5f) / travel));
    }

    /** Returns the maximum number of editor lines in the original viewport. */
    public static int visibleEditorLines() {
        return 35;
    }
}
