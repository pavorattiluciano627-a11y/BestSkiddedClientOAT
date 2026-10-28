package dev.echo.client.utils.geometry;

/** Direct coordinate operation used by the triangle-bin builder. */
public final class TriangleBounds {
    private TriangleBounds() {
    }

    /**
     * Returns the maximum coordinate of three interleaved two-dimensional vertices.
     * Array indexing, NaNs, and signed zero retain Java's Math.max semantics.
     *
     * @param triangle the interleaved vertex coordinates
     * @param axis the coordinate offset, normally zero or one
     * @return the maximum of offsets axis, axis+2, and axis+4
     */
    public static float maximumCoordinate(float[] triangle, int axis) {
        return Math.max(triangle[axis], Math.max(triangle[2 + axis], triangle[4 + axis]));
    }
}
