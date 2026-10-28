package dev.echo.client.render.math;

import org.joml.Vector4f;

/** Exact scalar operations shared by the backdrop, motion trail and projection. */
public final class RenderMath {
    private RenderMath() {
    }

    /**
     * Number of eight-pixel compute workgroups. Preserve Java integer arithmetic,
     * including its overflow and negative-input behavior; callers supply extents.
     */
    public static int workgroupsForExtent(int extent) {
        return (extent + 8 - 1) / 8;
    }

    /**
     * Quadratic 420-ms trail fade, at 35 percent of the supplied alpha.
     * Expired samples are removed by the owner, so this operation does not clamp.
     */
    public static int trailAlpha(int alpha, long nowMillis, long sampleMillis) {
        float remaining = 1.0F - (float) (nowMillis - sampleMillis) / 420.0F;
        return Math.round(alpha * 0.35F * remaining * remaining);
    }

    /** Pack the low eight bits of each component in Minecraft's ARGB order. */
    public static int packArgb(int red, int green, int blue, int alpha) {
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16
                | (green & 0xFF) << 8 | blue & 0xFF;
    }

    /** Interpolate homogeneous clip coordinates without modifying either input. */
    public static Vector4f interpolateClipPosition(Vector4f from, Vector4f to, float fraction) {
        return new Vector4f(
                from.x() + (to.x() - from.x()) * fraction,
                from.y() + (to.y() - from.y()) * fraction,
                from.z() + (to.z() - from.z()) * fraction,
                from.w() + (to.w() - from.w()) * fraction);
    }
}
