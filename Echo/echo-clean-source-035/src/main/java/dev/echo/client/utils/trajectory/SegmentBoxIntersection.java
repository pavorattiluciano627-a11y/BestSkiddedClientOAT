package dev.echo.client.utils.trajectory;

/**
 * Candidate six-face intersection from the launch search. This retains its
 * strict open segment and face-direction tests; it is not a slab replacement.
 * The original key/opaque-state branches still need proof before promotion.
 */
public final class SegmentBoxIntersection {
    private SegmentBoxIntersection() {
    }

    public static boolean intersects(double x, double y, double z,
            double deltaX, double deltaY, double deltaZ,
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (deltaX > 1.0E-7 && crossesFace(minX - x, deltaX,
                y, deltaY, z, deltaZ, minY, maxY, minZ, maxZ)) {
            return true;
        }
        if (deltaX < -1.0E-7 && crossesFace(maxX - x, deltaX,
                y, deltaY, z, deltaZ, minY, maxY, minZ, maxZ)) {
            return true;
        }
        if (deltaY > 1.0E-7 && crossesFace(minY - y, deltaY,
                x, deltaX, z, deltaZ, minX, maxX, minZ, maxZ)) {
            return true;
        }
        if (deltaY < -1.0E-7 && crossesFace(maxY - y, deltaY,
                x, deltaX, z, deltaZ, minX, maxX, minZ, maxZ)) {
            return true;
        }
        if (deltaZ > 1.0E-7 && crossesFace(minZ - z, deltaZ,
                x, deltaX, y, deltaY, minX, maxX, minY, maxY)) {
            return true;
        }
        return deltaZ < -1.0E-7 && crossesFace(maxZ - z, deltaZ,
                x, deltaX, y, deltaY, minX, maxX, minY, maxY);
    }

    private static boolean crossesFace(double faceDistance, double axisDelta,
            double firstStart, double firstDelta, double secondStart, double secondDelta,
            double firstMin, double firstMax, double secondMin, double secondMax) {
        double fraction = faceDistance / axisDelta;
        if (!(fraction > 0.0 && fraction < 1.0)) {
            return false;
        }
        double first = firstStart + fraction * firstDelta;
        double second = secondStart + fraction * secondDelta;
        return firstMin - 1.0E-7 < first && first < firstMax + 1.0E-7
                && secondMin - 1.0E-7 < second && second < secondMax + 1.0E-7;
    }
}
