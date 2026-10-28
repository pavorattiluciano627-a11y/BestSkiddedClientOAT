package dev.echo.client.chunk;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.EventBus;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.utils.ChunkCoordinates;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Arrays;
import net.minecraft.util.math.ChunkPos;

/**
 * Dense, indexed block-highlight sections and their renderer notifications.
 * The source preserves the original method-level locking and callback order.
 */
public class ChunkManager {
    private ChunkSectionData[] sections = new ChunkSectionData[4096];
    private int size;
    private final Long2IntOpenHashMap sectionIndices = new Long2IntOpenHashMap();
    private final int[] palette = new int[4096];
    private ChunkUpdateListener listener;
    private final int capacity = 200000;

    public ChunkManager() {
        sectionIndices.defaultReturnValue(-1);
        EventBus.register(this);
    }

    @Subscribe
    public void onClientTick(ClientTickEvent event) {
        rebuildDirtySections();
    }

    public void notifyPaletteChanged() {
        if (listener != null) {
            listener.onPaletteUpdated(palette);
        }
    }

    public synchronized void putSection(ChunkSectionData section) {
        // This check intentionally precedes replacement lookup in the original.
        if (size >= capacity) {
            return;
        }
        long key = ChunkSectionKey.packCoordinates(section.sectionX, section.sectionY, section.sectionZ);
        int index = sectionIndices.get(key);
        if (index != -1) {
            sections[index] = section;
            markSectionNeighborsDirty(section.sectionX, section.sectionY, section.sectionZ);
            if (listener != null) {
                listener.onSectionUpdated(index, section);
            }
            return;
        }
        if (size >= sections.length) {
            grow();
        }
        sections[size] = section;
        sectionIndices.put(key, size);
        size++;
        markSectionNeighborsDirty(section.sectionX, section.sectionY, section.sectionZ);
        if (listener != null) {
            listener.onSectionAdded(size - 1, section);
        }
    }

    public synchronized void setBlock(int blockX, int blockY, int blockZ, int blockId) {
        int sectionX = Math.floorDiv(blockX, 16);
        int sectionY = Math.floorDiv(blockY, 16);
        int sectionZ = Math.floorDiv(blockZ, 16);
        long key = ChunkSectionKey.packCoordinates(sectionX, sectionY, sectionZ);
        int index = sectionIndices.get(key);
        ChunkSectionData section;
        if (index == -1) {
            if (size >= capacity) {
                return;
            }
            if (size >= sections.length) {
                grow();
            }
            section = new ChunkSectionData(sectionX, sectionY, sectionZ);
            sections[size] = section;
            sectionIndices.put(key, size);
            size++;
            if (listener != null) {
                // The callback sees the newly allocated section before the first write.
                listener.onSectionAdded(size - 1, section);
            }
        } else {
            section = sections[index];
        }
        section.set(blockX & 15, blockY & 15, blockZ & 15, blockId);
        if (section.isDirty()) {
            markBlockNeighborsDirty(blockX, blockY, blockZ);
        }
    }

    public synchronized void removeBlock(int blockX, int blockY, int blockZ) {
        int sectionX = Math.floorDiv(blockX, 16);
        int sectionY = Math.floorDiv(blockY, 16);
        int sectionZ = Math.floorDiv(blockZ, 16);
        int index = sectionIndices.get(ChunkSectionKey.packCoordinates(sectionX, sectionY, sectionZ));
        if (index == -1) {
            return;
        }
        sections[index].set(blockX & 15, blockY & 15, blockZ & 15, 0);
        if (sections[index].isDirty()) {
            markBlockNeighborsDirty(blockX, blockY, blockZ);
        }
    }

    public synchronized void removeSection(int sectionX, int sectionY, int sectionZ) {
        long key = ChunkSectionKey.packCoordinates(sectionX, sectionY, sectionZ);
        int index = sectionIndices.get(key);
        if (index == -1) {
            return;
        }
        size--;
        if (index != size) {
            ChunkSectionData moved = sections[size];
            sections[index] = moved;
            sectionIndices.put(ChunkSectionKey.packCoordinates(moved.sectionX, moved.sectionY, moved.sectionZ), index);
            if (listener != null) {
                listener.onSectionAdded(index, moved);
            }
        }
        sections[size] = null;
        sectionIndices.remove(key);
        markSectionNeighborsDirty(sectionX, sectionY, sectionZ);
        if (listener != null) {
            listener.onSectionRemoved(size);
        }
    }

    public synchronized void removeChunk(ChunkPos chunk) {
        assert MinecraftAccess.CLIENT.world != null;
        // Preserve the original strict comparison against getTopYInclusive() >> 4.
        for (int sectionY = MinecraftAccess.CLIENT.world.getBottomY() >> 4;
                sectionY < (MinecraftAccess.CLIENT.world.getTopYInclusive() >> 4); sectionY++) {
            removeSection(ChunkCoordinates.getX(chunk), sectionY, ChunkCoordinates.getZ(chunk));
        }
    }

    public synchronized void rebuildDirtySections() {
        ChunkSectionData[] neighbors = new ChunkSectionData[6];
        for (int index = 0; index < size; index++) {
            ChunkSectionData section = sections[index];
            if (!section.isDirty()) {
                continue;
            }
            neighbors[0] = sectionAt(section.sectionX, section.sectionY - 1, section.sectionZ);
            neighbors[1] = sectionAt(section.sectionX, section.sectionY + 1, section.sectionZ);
            neighbors[2] = sectionAt(section.sectionX - 1, section.sectionY, section.sectionZ);
            neighbors[3] = sectionAt(section.sectionX + 1, section.sectionY, section.sectionZ);
            neighbors[4] = sectionAt(section.sectionX, section.sectionY, section.sectionZ - 1);
            neighbors[5] = sectionAt(section.sectionX, section.sectionY, section.sectionZ + 1);
            section.rebuild(neighbors);
            if (listener != null) {
                listener.onSectionUpdated(index, section);
            }
        }
    }

    private ChunkSectionData sectionAt(int sectionX, int sectionY, int sectionZ) {
        int index = sectionIndices.get(ChunkSectionKey.packCoordinates(sectionX, sectionY, sectionZ));
        return index == -1 ? null : sections[index];
    }

    private void markSectionNeighborsDirty(int sectionX, int sectionY, int sectionZ) {
        for (int face = 0; face < 6; face++) {
            int axis = face >> 1;
            int offset = (face & 1) == 0 ? -1 : 1;
            ChunkSectionData neighbor = sectionAt(sectionX + (axis == 1 ? offset : 0),
                    sectionY + (axis == 0 ? offset : 0), sectionZ + (axis == 2 ? offset : 0));
            if (neighbor != null) {
                neighbor.markDirty();
            }
        }
    }

    private void markBlockNeighborsDirty(int blockX, int blockY, int blockZ) {
        int sectionX = Math.floorDiv(blockX, 16);
        int sectionY = Math.floorDiv(blockY, 16);
        int sectionZ = Math.floorDiv(blockZ, 16);
        ChunkSectionData neighbor;
        if ((blockY & 15) == 0) {
            neighbor = sectionAt(sectionX, sectionY - 1, sectionZ);
            if (neighbor != null) { neighbor.markDirty(); }
        }
        if ((blockY & 15) == 15) {
            neighbor = sectionAt(sectionX, sectionY + 1, sectionZ);
            if (neighbor != null) { neighbor.markDirty(); }
        }
        if ((blockX & 15) == 0) {
            neighbor = sectionAt(sectionX - 1, sectionY, sectionZ);
            if (neighbor != null) { neighbor.markDirty(); }
        }
        if ((blockX & 15) == 15) {
            neighbor = sectionAt(sectionX + 1, sectionY, sectionZ);
            if (neighbor != null) { neighbor.markDirty(); }
        }
        if ((blockZ & 15) == 0) {
            neighbor = sectionAt(sectionX, sectionY, sectionZ - 1);
            if (neighbor != null) { neighbor.markDirty(); }
        }
        if ((blockZ & 15) == 15) {
            neighbor = sectionAt(sectionX, sectionY, sectionZ + 1);
            if (neighbor != null) { neighbor.markDirty(); }
        }
    }

    public synchronized void clear() {
        Arrays.fill(sections, 0, size, null);
        sectionIndices.clear();
        size = 0;
        if (listener != null) {
            listener.onCleared();
        }
    }

    public synchronized ChunkSectionData[] sections() { return sections; }
    public synchronized int size() { return size; }
    public synchronized boolean isEmpty() { return size == 0; }

    private void grow() {
        sections = Arrays.copyOf(sections, sections.length * 2);
    }

    public int[] palette() { return palette; }
    public void setListener(ChunkUpdateListener listener) { this.listener = listener; }
    public ChunkUpdateListener listener() { return listener; }
    public int capacity() { return capacity; }
}
