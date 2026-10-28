package dev.echo.render;

import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferInfo;
import dev.echo.render.device.RenderDevice;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.lwjgl.system.MemoryUtil;

/**
 * Mapped voxel storage with power-of-two spans and LIFO free lists.
 * Candidate only: key/guard/cache applicability is recorded in the unit receipt.
 */
public class VoxelBuffer {
    private static final int BYTES_PER_ELEMENT = 8;
    private static final int SIZE_CLASS_COUNT = 24;
    private static final long INITIAL_CAPACITY_BYTES = 65536L;

    private final RenderDevice device;
    private final LongArrayList[] freeOffsetsBySizeClass = new LongArrayList[SIZE_CLASS_COUNT];
    private Buffer buffer;
    private long allocatedBytes;

    public VoxelBuffer(RenderDevice device) {
        this.device = device;
        buffer = createBuffer(INITIAL_CAPACITY_BYTES);
        for (int sizeClass = 0; sizeClass < freeOffsetsBySizeClass.length; sizeClass++) {
            freeOffsetsBySizeClass[sizeClass] = new LongArrayList();
        }
    }

    /** Allocates a byte offset for an already-classified element count. */
    public long allocate(int sizeClass) {
        LongArrayList freeOffsets = freeOffsetsBySizeClass[sizeClass];
        if (!freeOffsets.isEmpty()) {
            return freeOffsets.removeLong(freeOffsets.size() - 1);
        }
        long spanBytes = (long) BYTES_PER_ELEMENT << sizeClass;
        if (allocatedBytes + spanBytes > buffer.size) {
            growBuffer(allocatedBytes + spanBytes);
        }
        long offset = allocatedBytes;
        allocatedBytes += spanBytes;
        return offset;
    }

    public void release(long offset, int sizeClass) {
        freeOffsetsBySizeClass[sizeClass].add(offset);
    }

    public void reset() {
        allocatedBytes = 0L;
        for (LongArrayList freeOffsets : freeOffsetsBySizeClass) {
            freeOffsets.clear();
        }
    }

    public void destroy() {
        device.release(buffer);
    }

    private void growBuffer(long requiredBytes) {
        long capacity = buffer.size;
        while (capacity < requiredBytes) {
            capacity <<= 1;
        }
        Buffer replacement = createBuffer(capacity);
        MemoryUtil.memCopy(buffer.mappedAddress(), replacement.mappedAddress(), allocatedBytes);
        replacement.flush(0L, allocatedBytes);
        device.release(buffer);
        buffer = replacement;
    }

    private Buffer createBuffer(long size) {
        return device.createBuffer(BufferInfo.builder().usage(1).size(size)
                .mapped(true).explicitFlush(true).build());
    }

    public Buffer getBuffer() {
        return buffer;
    }
}
