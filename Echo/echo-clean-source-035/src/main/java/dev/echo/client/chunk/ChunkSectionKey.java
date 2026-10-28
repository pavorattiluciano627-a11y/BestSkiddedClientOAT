package dev.echo.client.chunk;

/** The chunk index's three-coordinate, 21-bit-per-coordinate storage format. */
public final class ChunkSectionKey {
    private static final int COORDINATE_MASK = 0x1fffff;
    private static final int COORDINATE_BITS = 21;

    private ChunkSectionKey() {
    }

    /** Packs in caller order, retaining the low 21 bits even for negative inputs. */
    public static long packCoordinates(int highCoordinate, int middleCoordinate, int lowCoordinate) {
        return (long) (highCoordinate & COORDINATE_MASK) << (2 * COORDINATE_BITS)
                | (long) (middleCoordinate & COORDINATE_MASK) << COORDINATE_BITS
                | (long) (lowCoordinate & COORDINATE_MASK);
    }
}
