package dev.echo.client.chunk;

/** Receives mutations of the dense section array and its shared block palette. */
public interface ChunkUpdateListener {
    void onSectionAdded(int index, ChunkSectionData section);

    void onSectionUpdated(int index, ChunkSectionData section);

    /** Removes the old last array slot after any swap-in notification. */
    void onSectionRemoved(int lastIndex);

    void onCleared();

    /** The palette is the manager's original mutable array, not a copy. */
    void onPaletteUpdated(int[] palette);
}
