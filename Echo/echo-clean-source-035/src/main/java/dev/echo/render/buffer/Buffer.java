package dev.echo.render.buffer;

import dev.echo.render.resource.GpuResource;
import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;
import java.util.Collection;

/** GPU storage with independent access-range tracking for each in-flight frame. */
public abstract class Buffer extends GpuResource {
    public final int usage;
    public final long size;
    public final boolean dynamic;
    public final boolean mapped;
    private final Long2ObjectRBTreeMap<BufferRangeState>[] frameRangeStates;

    @SuppressWarnings("unchecked")
    public Buffer(int usage, long size, boolean dynamic, boolean mapped, int frameCount) {
        this.usage = usage;
        this.size = size;
        this.dynamic = dynamic;
        this.mapped = mapped;
        frameRangeStates = (Long2ObjectRBTreeMap<BufferRangeState>[]) new Long2ObjectRBTreeMap<?>[frameCount];
        for (int frame = 0; frame < frameCount; frame++) {
            frameRangeStates[frame] = new Long2ObjectRBTreeMap<>();
            frameRangeStates[frame].put(0L, new BufferRangeState(0, size, BufferAccess.UNDEFINED, 16));
        }
    }

    protected abstract int frameIndex();

    private Long2ObjectRBTreeMap<BufferRangeState> rangeStates() {
        return frameRangeStates[frameIndex()];
    }

    public abstract long mappedAddress();

    public abstract void flush(long offset, long length);

    public BufferAccess accessAt(long offset) {
        Long2ObjectRBTreeMap<BufferRangeState> states = rangeStates();
        Long2ObjectSortedMap<BufferRangeState> preceding = states.headMap(offset + 1);
        if (preceding.isEmpty()) {
            return BufferAccess.UNDEFINED;
        }
        return states.get(preceding.lastLongKey()).access;
    }

    /** Returns the original live map-values view, including the range containing offset. */
    public Collection<BufferRangeState> overlappingRanges(long offset, long length) {
        Long2ObjectRBTreeMap<BufferRangeState> states = rangeStates();
        long first = states.headMap(offset + 1).lastLongKey();
        return states.subMap(first, offset + length).values();
    }

    public void setRangeState(long offset, long length, BufferAccess access, int stages) {
        if (length <= 0) {
            return;
        }
        Long2ObjectRBTreeMap<BufferRangeState> states = rangeStates();
        long end = offset + length;
        splitRangeAt(offset);
        splitRangeAt(end);
        states.subMap(offset, end).clear();
        states.put(offset, new BufferRangeState(offset, length, access, stages));
        mergeAdjacentRanges(offset);
        mergeAdjacentRanges(end);
    }

    private void splitRangeAt(long offset) {
        if (offset <= 0 || offset >= size) {
            return;
        }
        Long2ObjectRBTreeMap<BufferRangeState> states = rangeStates();
        long first = states.headMap(offset + 1).lastLongKey();
        if (first == offset) {
            return;
        }
        BufferRangeState preceding = states.get(first);
        long trailingLength = preceding.endOffset() - offset;
        preceding.length = offset - preceding.offset;
        states.put(offset, new BufferRangeState(offset, trailingLength, preceding.access, preceding.stages));
    }

    private void mergeAdjacentRanges(long offset) {
        Long2ObjectRBTreeMap<BufferRangeState> states = rangeStates();
        BufferRangeState current = states.get(offset);
        Long2ObjectSortedMap<BufferRangeState> precedingRanges = states.headMap(offset);
        if (current == null || precedingRanges.isEmpty()) {
            return;
        }
        BufferRangeState preceding = states.get(precedingRanges.lastLongKey());
        if (preceding.endOffset() == current.offset
                && preceding.access == current.access && preceding.stages == current.stages) {
            preceding.length += current.length;
            states.remove(current.offset);
        }
    }

    public BufferSlice slice(long offset, long length) {
        return new BufferSlice(this, offset, length);
    }

    public BufferSlice wholeSlice() {
        return new BufferSlice(this, 0, size);
    }
}
