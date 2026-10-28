package dev.echo.client.render;

/**
 * Message queued by chunk callbacks and drained by the rendering thread.
 * Producers copy the packed-face and per-direction face-count arrays before enqueueing.
 * The record itself intentionally keeps the supplied array references.
 */
public record SectionMeshUpdate(int operation, int sectionIndex, int[] packedFaces,
                                int[] faceCounts, int sectionX, int sectionY, int sectionZ) {
    public static final int UPSERT = 0;
    public static final int REMOVE = 1;
    public static final int CLEAR = 2;
}
