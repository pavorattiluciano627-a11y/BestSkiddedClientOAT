package dev.echo.render.util;

import org.lwjgl.system.MemoryUtil;

/**
 * Direct writes to a borrowed native allocation. The caller owns the allocation,
 * its bounds, its lifetime, and any GPU visibility/flush requirements.
 */
public final class NativeMemoryWrites {
    private NativeMemoryWrites() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static void putFloat(long address, float value) {
        MemoryUtil.memPutFloat(address, value);
    }

    public static void putFloat2(long address, float x, float y) {
        MemoryUtil.memPutFloat(address, x);
        MemoryUtil.memPutFloat(address + Float.BYTES, y);
    }

    public static void putFloat3(long address, float x, float y, float z) {
        MemoryUtil.memPutFloat(address, x);
        MemoryUtil.memPutFloat(address + Float.BYTES, y);
        MemoryUtil.memPutFloat(address + 2L * Float.BYTES, z);
    }

    public static void putInt(long address, int value) {
        MemoryUtil.memPutInt(address, value);
    }

    public static void putInt2(long address, int x, int y) {
        MemoryUtil.memPutInt(address, x);
        MemoryUtil.memPutInt(address + Integer.BYTES, y);
    }

    public static void putInt3(long address, int x, int y, int z) {
        MemoryUtil.memPutInt(address, x);
        MemoryUtil.memPutInt(address + Integer.BYTES, y);
        MemoryUtil.memPutInt(address + 2L * Integer.BYTES, z);
    }

    public static void putInt4(long address, int x, int y, int z, int w) {
        putInt3(address, x, y, z);
        MemoryUtil.memPutInt(address + 3L * Integer.BYTES, w);
    }

    public static void copy(long source, long destination, long byteCount) {
        MemoryUtil.memCopy(source, destination, byteCount);
    }

    public static void fill(long address, byte value, long byteCount) {
        MemoryUtil.memSet(address, value, byteCount);
    }
}
