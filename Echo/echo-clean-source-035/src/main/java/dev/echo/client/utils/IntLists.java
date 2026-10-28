package dev.echo.client.utils;

import dev.echo.client.utils.geometry.TriangleBounds;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.List;

/** Builds spatial bins of triangle indices for the original two-dimensional geometry. */
public final class IntLists {
    private static final int BIN_COUNT = 16;

    private IntLists() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Appends sixteen bin ranges and their sorted triangle indices.
     * Triangles touching a bin boundary occur in both adjacent bins. Coordinates
     * are interleaved x,y pairs; each bin sorts by descending maximum on the other
     * axis. Existing range/index entries and integer-overflow behavior are retained.
     *
     * @param triangles triangle arrays, with three interleaved coordinate pairs each
     * @param triangleIndexOffset offset added to each appended triangle index
     * @param axis the binning coordinate offset, normally zero or one
     * @param ranges destination start/count pairs
     * @param indices destination flattened index list
     * @return the range-list size before these bins were appended
     */
    public static int appendBins(List<float[]> triangles, int triangleIndexOffset, int axis,
                                  List<int[]> ranges, IntArrayList indices) {
        int sortAxis = 1 - axis;
        int firstRange = ranges.size();
        for (int bin = 0; bin < BIN_COUNT; bin++) {
            IntArrayList members = intersectingTriangles(triangles, axis, bin);
            members.sort((int first, int second) -> Float.compare(
                    TriangleBounds.maximumCoordinate(triangles.get(second), sortAxis),
                    TriangleBounds.maximumCoordinate(triangles.get(first), sortAxis)));
            int start = indices.size();
            for (int triangle : members) indices.add(triangleIndexOffset + triangle);
            ranges.add(new int[]{start, members.size()});
        }
        return firstRange;
    }

    private static IntArrayList intersectingTriangles(List<float[]> triangles, int axis, int bin) {
        float lower = (float) bin / BIN_COUNT;
        float upper = (float) (bin + 1) / BIN_COUNT;
        IntArrayList result = new IntArrayList();
        for (int index = 0; index < triangles.size(); index++) {
            float[] triangle = triangles.get(index);
            float minimum = Math.min(triangle[axis], Math.min(triangle[2 + axis], triangle[4 + axis]));
            float maximum = Math.max(triangle[axis], Math.max(triangle[2 + axis], triangle[4 + axis]));
            if (maximum >= lower && minimum <= upper) result.add(index);
        }
        return result;
    }
}
