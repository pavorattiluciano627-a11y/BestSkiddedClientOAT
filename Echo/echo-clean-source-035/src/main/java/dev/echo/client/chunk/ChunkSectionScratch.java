package dev.echo.client.chunk;

/**
 * Complete buffer/thread-local source candidate for ValueBuilder. Array sizes
 * and allocation order are retained. The original constructor's control-state
 * side effect and dispatch/initialization applicability still require proof.
 */
final class ChunkSectionScratch {
    private static final ThreadLocal<ChunkSectionScratch> SCRATCH =
            ThreadLocal.withInitial(ChunkSectionScratch::new);

    final short[] expandedStates = new short[4096];
    final int[] parents = new int[4096];
    final int[] sumX = new int[4096];
    final int[] sumY = new int[4096];
    final int[] sumZ = new int[4096];
    final int[] counts = new int[4096];
    final int[] occupancyColumns = new int[972];
    final int[] faceColumns = new int[1944];
    final int[] facePlane = new int[256];

    // Origin: hack/ValueBuilder.removeView(J)Lhack/ValueBuilder;.
    static ChunkSectionScratch current() {
        return SCRATCH.get();
    }
}
