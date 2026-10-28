package dev.echo.render;

/** Power-of-two size classes used by the voxel buffer allocator. */
public final class VoxelAllocationSizes {
    private VoxelAllocationSizes() {
    }

    /**
     * Returns the power-of-two element-count exponent, with a minimum of four.
     * The original int subtraction and leading-zero semantics are retained,
     * including overflow and nonpositive inputs. Each element occupies eight bytes.
     */
    public static int sizeClassFor(int elementCount) {
        return Math.max(4, 32 - Integer.numberOfLeadingZeros(elementCount - 1));
    }
}
