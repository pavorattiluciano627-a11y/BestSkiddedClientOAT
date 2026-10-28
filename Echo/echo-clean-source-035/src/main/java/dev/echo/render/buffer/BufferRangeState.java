package dev.echo.render.buffer;

/** Mutable access state for a half-open byte range of a buffer. */
public class BufferRangeState {
    public long offset;
    public long length;
    public BufferAccess access;
    public int stages;

    public BufferRangeState(long offset, long length, BufferAccess access, int stages) {
        this.offset = offset;
        this.length = length;
        this.access = access;
        this.stages = stages;
    }

    public long endOffset() {
        return offset + length;
    }
}
