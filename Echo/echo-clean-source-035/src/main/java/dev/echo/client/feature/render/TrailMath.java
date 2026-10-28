package dev.echo.client.feature.render;

/** Numerical operations used to build and fade the trail ribbon. */
public final class TrailMath {
    private TrailMath() {
    }

    /** Returns the squared distance, retaining the original operation order. */
    public static double squaredDistance(double x1, double y1, double z1,
                                         double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Packs the low eight bits of each channel; out-of-range values wrap. */
    public static int packArgb(int red, int green, int blue, int alpha) {
        return (alpha & 255) << 24 | (red & 255) << 16
            | (green & 255) << 8 | blue & 255;
    }

    /** Fades using float arithmetic, including its NaN and overflow behavior. */
    public static int fadeAlpha(int alpha, long nowMillis,
                                long sampleMillis, long lifetimeMillis) {
        float remaining = 1.0f - (float) (nowMillis - sampleMillis) / (float) lifetimeMillis;
        return Math.round((float) alpha * Math.max(0.0f, Math.min(1.0f, remaining)));
    }
}
