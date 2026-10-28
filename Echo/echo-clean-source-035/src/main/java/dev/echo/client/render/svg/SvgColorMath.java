package dev.echo.client.render.svg;

/** Color operations used by the SVG texture resampler. */
public final class SvgColorMath {
    private SvgColorMath() {
    }

    /** Clamp a rounded channel to the unsigned eight-bit range. */
    public static int clampChannel(int channel) {
        return Math.max(0, Math.min(255, channel));
    }

    /** Convert an eight-bit sRGB channel to linear light before averaging. */
    public static double srgbToLinear(int channel) {
        double srgb = channel / 255.0;
        if (srgb <= 0.04045) {
            return srgb / 12.92;
        }
        return Math.pow((srgb + 0.055) / 1.055, 2.4);
    }
}
