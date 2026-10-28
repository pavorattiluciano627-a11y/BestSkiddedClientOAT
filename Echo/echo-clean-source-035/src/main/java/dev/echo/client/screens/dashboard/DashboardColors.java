package dev.echo.client.screens.dashboard;

/** Color calculations used by the original dashboard's picker and transitions. */
public final class DashboardColors {
    private DashboardColors() {
    }

    /** Returns the HSV value component of an RGB/ARGB color; alpha is ignored. */
    public static float brightness(int color) {
        return Math.max(color >> 16 & 0xff,
                Math.max(color >> 8 & 0xff, color & 0xff)) / 255.0f;
    }

    /** Interpolates all four ARGB bytes independently, using Java's rounding. */
    public static int interpolate(int from, int to, float progress) {
        float amount = Math.max(0.0f, Math.min(1.0f, progress));
        int alpha = Math.round((from >>> 24 & 0xff)
                + ((to >>> 24 & 0xff) - (from >>> 24 & 0xff)) * amount);
        int red = Math.round((from >>> 16 & 0xff)
                + ((to >>> 16 & 0xff) - (from >>> 16 & 0xff)) * amount);
        int green = Math.round((from >>> 8 & 0xff)
                + ((to >>> 8 & 0xff) - (from >>> 8 & 0xff)) * amount);
        int blue = Math.round((from & 0xff) + ((to & 0xff) - (from & 0xff)) * amount);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    /** Scales alpha without clamping, preserving the original overflow behavior. */
    public static int multiplyAlpha(int color, float opacity) {
        int alpha = Math.round((color >>> 24 & 0xff) * opacity);
        return color & 0x00ffffff | alpha << 24;
    }
}
