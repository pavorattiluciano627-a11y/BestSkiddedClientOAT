package dev.echo.client.ui;

/**
 * Letterboxed logical-canvas coordinates and their uniform screen transform.
 * Source-complete candidate: contains() still needs the UiId/cache applicability domain.
 *
 * @param logicalWidth canvas width in logical units
 * @param logicalHeight canvas height in logical units
 * @param screenWidth available screen width
 * @param screenHeight available screen height
 * @param scale uniform scale from logical to screen coordinates
 * @param offsetX horizontal letterbox offset
 * @param offsetY vertical letterbox offset
 */
public record CanvasMetrics(float logicalWidth, float logicalHeight,
                            float screenWidth, float screenHeight,
                            float scale, float offsetX, float offsetY) {
    /** Fits a logical canvas into screen bounds without stretching its aspect ratio. */
    public static CanvasMetrics fit(float screenWidth, float screenHeight,
                                    float logicalWidth, float logicalHeight) {
        if (!(screenWidth > 0.0f) || !(screenHeight > 0.0f)
                || !(logicalWidth > 0.0f) || !(logicalHeight > 0.0f)) {
            throw new IllegalArgumentException("Canvas extents must be positive");
        }
        float scale = Math.min(screenWidth / logicalWidth, screenHeight / logicalHeight);
        return new CanvasMetrics(logicalWidth, logicalHeight, screenWidth, screenHeight,
                scale, (screenWidth - logicalWidth * scale) * 0.5f,
                (screenHeight - logicalHeight * scale) * 0.5f);
    }

    /** Converts a logical x coordinate to screen space. */
    public float toScreenX(float x) {
        return offsetX + x * scale;
    }

    /** Converts a logical y coordinate to screen space. */
    public float toScreenY(float y) {
        return offsetY + y * scale;
    }

    /** Converts a logical distance to screen space. */
    public float toScreenLength(float length) {
        return length * scale;
    }

    /** Converts a screen x coordinate to logical space. */
    public float toLogicalX(float x) {
        return (x - offsetX) / scale;
    }

    /** Converts a screen y coordinate to logical space. */
    public float toLogicalY(float y) {
        return (y - offsetY) / scale;
    }

    /** Transforms a logical rectangle without rounding its edges. */
    public UiRect toScreen(UiRect rectangle) {
        return new UiRect(toScreenX(rectangle.x()), toScreenY(rectangle.y()),
                toScreenLength(rectangle.width()), toScreenLength(rectangle.height()));
    }

    /** Checks logical coordinates against the inclusive canvas bounds. */
    public boolean contains(float x, float y) {
        return x >= 0.0f && y >= 0.0f && x <= logicalWidth && y <= logicalHeight;
    }
}
