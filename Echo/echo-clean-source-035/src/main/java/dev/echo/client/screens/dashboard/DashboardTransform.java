package dev.echo.client.screens.dashboard;

/**
 * Snapshot of the original dashboard's render transform.
 * This preserves its distinct horizontal and vertical animation offsets.
 *
 * @param viewportLeft physical horizontal viewport origin
 * @param viewportTop physical vertical viewport origin
 * @param viewportScale scale from 1920 by 1080 logical coordinates
 * @param zoom additional dashboard scale
 * @param animationScale local animation scale
 * @param pivotX logical horizontal animation pivot
 * @param pivotY logical vertical animation pivot
 * @param localOffsetX horizontal offset applied after pivot interpolation
 * @param localOffsetY vertical offset applied after pivot interpolation
 * @param verticalOffset additional vertical viewport-scaled translation
 */
public record DashboardTransform(
        float viewportLeft, float viewportTop, float viewportScale, float zoom,
        float animationScale, float pivotX, float pivotY,
        float localOffsetX, float localOffsetY, float verticalOffset) {

    /** Converts a logical X coordinate to the original physical render position. */
    public float renderX(float logicalX) {
        float center = viewportLeft + 1920.0f * viewportScale * 0.5f;
        float animated = pivotX + (logicalX - pivotX) * animationScale;
        return center + (animated - 960.0f + localOffsetX) * viewportScale * zoom;
    }

    /** Converts a logical Y coordinate to the original physical render position. */
    public float renderY(float logicalY) {
        float center = viewportTop + 1080.0f * viewportScale * 0.5f;
        float animated = pivotY + (logicalY - pivotY) * animationScale + localOffsetY;
        return center + (animated - 540.0f) * viewportScale * zoom + verticalOffset * viewportScale;
    }

    /** Applies all three scales to a logical distance. */
    public float renderSize(float logicalSize) {
        return logicalSize * viewportScale * zoom * animationScale;
    }
}
