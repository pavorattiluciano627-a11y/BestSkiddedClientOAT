package dev.echo.client.render;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

/** Direct, native-order writes into caller-owned mapped rendering memory. */
public final class NativeMemoryWrites {
    private NativeMemoryWrites() {}

    /** Writes a column-major mat4; the destination must contain at least 64 bytes. */
    public static void putMatrix4f(long address, Matrix4f matrix) {
        MemoryUtil.memPutFloat(address, matrix.m00());
        MemoryUtil.memPutFloat(address + 4, matrix.m01());
        MemoryUtil.memPutFloat(address + 8, matrix.m02());
        MemoryUtil.memPutFloat(address + 12, matrix.m03());
        MemoryUtil.memPutFloat(address + 16, matrix.m10());
        MemoryUtil.memPutFloat(address + 20, matrix.m11());
        MemoryUtil.memPutFloat(address + 24, matrix.m12());
        MemoryUtil.memPutFloat(address + 28, matrix.m13());
        MemoryUtil.memPutFloat(address + 32, matrix.m20());
        MemoryUtil.memPutFloat(address + 36, matrix.m21());
        MemoryUtil.memPutFloat(address + 40, matrix.m22());
        MemoryUtil.memPutFloat(address + 44, matrix.m23());
        MemoryUtil.memPutFloat(address + 48, matrix.m30());
        MemoryUtil.memPutFloat(address + 52, matrix.m31());
        MemoryUtil.memPutFloat(address + 56, matrix.m32());
        MemoryUtil.memPutFloat(address + 60, matrix.m33());
    }

    public static void putFloat3(long address, float x, float y, float z) {
        MemoryUtil.memPutFloat(address, x);
        MemoryUtil.memPutFloat(address + 4, y);
        MemoryUtil.memPutFloat(address + 8, z);
    }

    public static void putFloat2(long address, float x, float y) {
        MemoryUtil.memPutFloat(address, x);
        MemoryUtil.memPutFloat(address + 4, y);
    }

    public static void putFloat4(long address, float x, float y, float z, float w) {
        MemoryUtil.memPutFloat(address, x);
        MemoryUtil.memPutFloat(address + 4, y);
        MemoryUtil.memPutFloat(address + 8, z);
        MemoryUtil.memPutFloat(address + 12, w);
    }

    public static void putInt(long address, int value) {
        MemoryUtil.memPutInt(address, value);
    }

    public static void putInt2(long address, int x, int y) {
        MemoryUtil.memPutInt(address, x);
        MemoryUtil.memPutInt(address + 4, y);
    }

    public static void putInt3(long address, int x, int y, int z) {
        MemoryUtil.memPutInt(address, x);
        MemoryUtil.memPutInt(address + 4, y);
        MemoryUtil.memPutInt(address + 8, z);
    }

    public static void putFloat(long address, float value) {
        MemoryUtil.memPutFloat(address, value);
    }

    public static void copy(long sourceAddress, long destinationAddress, long byteCount) {
        MemoryUtil.memCopy(sourceAddress, destinationAddress, byteCount);
    }

    public static void fill(long address, byte value, long byteCount) {
        MemoryUtil.memSet(address, value, byteCount);
    }
}
