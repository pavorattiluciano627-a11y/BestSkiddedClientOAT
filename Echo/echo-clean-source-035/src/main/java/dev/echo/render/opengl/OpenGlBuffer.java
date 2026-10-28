package dev.echo.render.opengl;

import dev.echo.render.buffer.Buffer;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GL40C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.system.MemoryUtil;

/** GL buffer plus optional CPU staging allocation; mapping is not a GL persistent map. */
public class OpenGlBuffer extends Buffer {
    public final int bufferId;
    public final int target;
    private final long mappedAddress;
    private final boolean explicitFlush;
    private long dirtyStart = Long.MAX_VALUE;
    private long dirtyEnd;

    public OpenGlBuffer(int usage, long size, boolean dynamic, boolean mapped, boolean explicitFlush) {
        super(usage, size, dynamic, mapped, 1);
        mappedAddress = mapped ? MemoryUtil.nmemAlloc(size) : 0;
        this.explicitFlush = explicitFlush;
        if ((usage & 4) == 0) {
            target = glTarget(usage);
            bufferId = GL15C.glGenBuffers();
            GL15C.glBindBuffer(target, bufferId);
            GL15C.glBufferData(target, size, dynamic ? GL15C.GL_DYNAMIC_DRAW : GL15C.GL_STATIC_DRAW);
            GL15C.glBindBuffer(target, 0);
        } else {
            // Transfer-source buffers are CPU staging allocations on this backend.
            bufferId = 0;
            target = 0;
        }
    }

    private static int glTarget(int usage) {
        if ((usage & 1) != 0) return GL15C.GL_ARRAY_BUFFER;
        if ((usage & 32) != 0) return GL15C.GL_ELEMENT_ARRAY_BUFFER;
        if ((usage & 2) != 0) return GL31C.GL_UNIFORM_BUFFER;
        if ((usage & 8) != 0) return GL40C.GL_DRAW_INDIRECT_BUFFER;
        if ((usage & 16) != 0) return GL43C.GL_SHADER_STORAGE_BUFFER;
        throw new IllegalStateException("Unsupported buffer usage for GL target: " + usage);
    }

    @Override
    public long mappedAddress() {
        return mappedAddress;
    }

    @Override
    public void flush(long offset, long length) {
        if (length <= 0) return;
        dirtyStart = Math.min(dirtyStart, offset);
        dirtyEnd = Math.max(dirtyEnd, offset + length);
    }

    /** Uploads to an already bound GL target. Dirty ranges are shared across slice bindings. */
    void uploadStagedData(int boundTarget, long offset, long length) {
        if (!mapped) return;
        if (!explicitFlush) {
            GL15C.nglBufferSubData(boundTarget, offset, length, mappedAddress + offset);
            return;
        }
        if (dirtyStart >= dirtyEnd) return;
        GL15C.nglBufferSubData(boundTarget, dirtyStart, dirtyEnd - dirtyStart, mappedAddress + dirtyStart);
        dirtyStart = Long.MAX_VALUE;
        dirtyEnd = 0;
    }

    @Override
    protected int frameIndex() {
        return 0;
    }

    @Override
    public void destroy() {
        GL15C.glDeleteBuffers(bufferId);
        if (mappedAddress != 0) MemoryUtil.nmemFree(mappedAddress);
    }
}
