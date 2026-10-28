package dev.echo.render.vulkan;

import dev.echo.render.buffer.BufferAccess;
import dev.echo.render.texture.TextureAccess;
import dev.echo.render.texture.TextureLayout;

/** Stage selection policy, separate from the native access-mask conversions. */
public final class VulkanSynchronization {
    private VulkanSynchronization() {}

    public static int imageLayout(TextureLayout layout) { return VulkanAccessMasks.imageLayout(layout); }

    public static boolean writes(BufferAccess access) {
        return access == BufferAccess.STORAGE_WRITE || access == BufferAccess.TRANSFER_WRITE;
    }

    public static boolean writes(TextureAccess access) {
        return access == TextureAccess.STORAGE_WRITE || access == TextureAccess.TRANSFER_WRITE
                || access == TextureAccess.COLOR_ATTACHMENT || access == TextureAccess.DEPTH_ATTACHMENT;
    }

    public static int shaderStages(int stages) {
        int result = 0;
        if ((stages & 16) != 0) result |= 1;
        if ((stages & 1) != 0) result |= 8;
        if ((stages & 2) != 0) result |= 128;
        if ((stages & 4) != 0) result |= 2048;
        if ((stages & 8) != 0) result |= 32768;
        return result;
    }

    public static int bufferStages(BufferAccess access, int stages) {
        return switch (access) {
            case UNDEFINED -> 1;
            case VERTEX_READ, INDEX_READ -> 4;
            case INDIRECT_READ -> 2;
            case TRANSFER_READ, TRANSFER_WRITE -> 4096;
            case UNIFORM_READ -> stages == 0 ? 136 : shaderStages(stages);
            case STORAGE_READ, STORAGE_WRITE -> stages == 0 ? 2048 : shaderStages(stages);
        };
    }

    public static int textureStages(TextureAccess access, int stages) {
        return switch (access) {
            case UNDEFINED -> 1;
            case PRESENT -> 8192;
            case COLOR_ATTACHMENT -> 1024;
            case DEPTH_ATTACHMENT -> 768;
            case TRANSFER_READ, TRANSFER_WRITE -> 4096;
            case SAMPLED -> stages == 0 ? 128 : shaderStages(stages);
            case STORAGE_READ, STORAGE_WRITE -> stages == 0 ? 2048 : shaderStages(stages);
        };
    }
}
