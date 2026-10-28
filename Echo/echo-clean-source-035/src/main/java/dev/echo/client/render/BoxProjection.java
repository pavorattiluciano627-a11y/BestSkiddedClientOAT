package dev.echo.client.render;

import dev.echo.client.render.math.RenderMath;
import org.joml.Vector4f;

/**
 * Reconstructed clipping stage of the world-to-screen box projection.
 * WorldProjection supplies camera/event acquisition and retains the shared-state branch.
 */
public final class BoxProjection {
    private static final float NEAR_W = 0.05F;

    private BoxProjection() {
    }

    /**
     * Corners are ordered around the lower face, then the upper face, each as
     * (minX,minZ), (maxX,minZ), (maxX,maxZ), (minX,maxZ). They have already been
     * translated by the camera and multiplied by the view, then projection.
     * Clip coordinates are not mutated. Bounds are not clamped to the viewport.
     */
    public static ProjectedBounds projectClipCorners(Vector4f[] corners, int width, int height) {
        int[][] edges = orderedEdges();
        ProjectedBounds bounds = null;
        boolean visible = false;
        for (int[] edge : edges) {
            Vector4f from = corners[edge[0]];
            Vector4f to = corners[edge[1]];
            boolean fromVisible = from.w() > NEAR_W;
            boolean toVisible = to.w() > NEAR_W;
            if (!fromVisible && !toVisible) {
                continue;
            }

            Vector4f clippedFrom = from;
            Vector4f clippedTo = to;
            if (!fromVisible) {
                float fraction = (NEAR_W - from.w()) / (to.w() - from.w());
                clippedFrom = RenderMath.interpolateClipPosition(from, to, fraction);
            } else if (!toVisible) {
                float fraction = (NEAR_W - from.w()) / (to.w() - from.w());
                clippedTo = RenderMath.interpolateClipPosition(from, to, fraction);
            }
            visible = true;

            // Division is deliberately float before conversion to double.
            double fromX = ((double) (clippedFrom.x() / clippedFrom.w()) * 0.5 + 0.5) * width;
            double fromY = (0.5 - (double) (clippedFrom.y() / clippedFrom.w()) * 0.5) * height;
            double toX = ((double) (clippedTo.x() / clippedTo.w()) * 0.5 + 0.5) * width;
            double toY = (0.5 - (double) (clippedTo.y() / clippedTo.w()) * 0.5) * height;

            if (bounds == null) {
                bounds = new ProjectedBounds(fromX, fromY, fromX, fromY);
            }
            // Preserve comparisons rather than Math.min/max (different NaN behavior).
            if (fromX < bounds.minX) bounds.minX = fromX;
            if (fromY < bounds.minY) bounds.minY = fromY;
            if (fromX > bounds.maxX) bounds.maxX = fromX;
            if (fromY > bounds.maxY) bounds.maxY = fromY;
            if (toX < bounds.minX) bounds.minX = toX;
            if (toY < bounds.minY) bounds.minY = toY;
            if (toX > bounds.maxX) bounds.maxX = toX;
            if (toY > bounds.maxY) bounds.maxY = toY;
        }
        if (bounds == null) {
            bounds = new ProjectedBounds(0.0, 0.0, 0.0, 0.0);
        }
        bounds.visible = visible;
        return bounds;
    }

    /** A fresh array preserves the original per-projection allocation and edge order. */
    static int[][] orderedEdges() {
        return new int[][]{
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
    }
}
