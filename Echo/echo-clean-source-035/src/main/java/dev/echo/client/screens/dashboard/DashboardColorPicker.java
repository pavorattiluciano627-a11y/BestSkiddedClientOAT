package dev.echo.client.screens.dashboard;

/** The original dashboard's RGB/HSV conversions under its recorded screen-guard domain. */
public final class DashboardColorPicker {
    private DashboardColorPicker() {
    }

    /** Returns the HSV saturation, ignoring alpha and treating black as zero. */
    public static float saturation(int color) {
        float value = DashboardColors.brightness(color);
        if (value <= 0.0f) return 0.0f;
        float minimum = Math.min(color >> 16 & 0xff,
                Math.min(color >> 8 & 0xff, color & 0xff)) / 255.0f;
        return (value - minimum) / value;
    }

    /** Computes hue using the original red-first equality and near-gray threshold. */
    public static float hue(int color) {
        float red = (color >> 16 & 0xff) / 255.0f;
        float green = (color >> 8 & 0xff) / 255.0f;
        float blue = (color & 0xff) / 255.0f;
        float maximum = Math.max(red, Math.max(green, blue));
        float difference = maximum - Math.min(red, Math.min(green, blue));
        if (difference <= 1.0e-5f) return 0.0f;
        float hue;
        if (maximum == red) hue = (green - blue) / difference % 6.0f;
        else if (maximum == green) hue = (blue - red) / difference + 2.0f;
        else hue = (red - green) / difference + 4.0f;
        hue /= 6.0f;
        return hue < 0.0f ? hue + 1.0f : hue;
    }

    /**
     * Converts HSV and opacity to ARGB. Only opacity is clamped, preserving the
     * original behavior for out-of-range saturation/value and Java rounding.
     */
    public static int argb(float hue, float saturation, float value, float opacity) {
        float sectors = (hue - (float) Math.floor(hue)) * 6.0f;
        int sector = (int) Math.floor(sectors);
        float fraction = sectors - sector;
        float minimum = value * (1.0f - saturation);
        float descending = value * (1.0f - saturation * fraction);
        float ascending = value * (1.0f - saturation * (1.0f - fraction));
        float red;
        float green;
        float blue;
        switch (sector) {
            case 0 -> { red = value; green = ascending; blue = minimum; }
            case 1 -> { red = descending; green = value; blue = minimum; }
            case 2 -> { red = minimum; green = value; blue = ascending; }
            case 3 -> { red = minimum; green = descending; blue = value; }
            case 4 -> { red = ascending; green = minimum; blue = value; }
            default -> { red = value; green = minimum; blue = descending; }
        }
        return Math.round(Math.max(0.0f, Math.min(1.0f, opacity)) * 255.0f) << 24
                | Math.round(red * 255.0f) << 16 | Math.round(green * 255.0f) << 8
                | Math.round(blue * 255.0f);
    }
}
