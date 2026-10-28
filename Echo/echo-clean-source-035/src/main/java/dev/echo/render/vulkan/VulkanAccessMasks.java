package dev.echo.render.vulkan;

import dev.echo.render.buffer.BufferAccess;
import dev.echo.render.texture.TextureAccess;
import dev.echo.render.texture.TextureLayout;

/** Exact Vulkan 1.0 masks and layouts from the original enum conversion tables. */
public final class VulkanAccessMasks {
    private VulkanAccessMasks() {}

    public static int bufferAccessMask(BufferAccess access) {
        return switch (access) {
            case UNDEFINED -> 0;
            case VERTEX_READ -> 4;
            case INDEX_READ -> 2;
            case UNIFORM_READ -> 8;
            case INDIRECT_READ -> 1;
            case STORAGE_READ -> 32;
            case STORAGE_WRITE -> 64;
            case TRANSFER_READ -> 2048;
            case TRANSFER_WRITE -> 4096;
        };
    }

    public static int textureAccessMask(TextureAccess access) {
        return switch (access) {
            case UNDEFINED, PRESENT -> 0;
            case SAMPLED, STORAGE_READ -> 32;
            case STORAGE_WRITE -> 64;
            case COLOR_ATTACHMENT -> 384;
            case DEPTH_ATTACHMENT -> 1536;
            case TRANSFER_READ -> 2048;
            case TRANSFER_WRITE -> 4096;
        };
    }

    public static int imageLayout(TextureLayout layout) {
        if (layout == null) return 0;
        return switch (layout) {
            case UNDEFINED -> 0;
            case GENERAL -> 1;
            case SHADER_READ_ONLY -> 5;
            case COLOR_ATTACHMENT -> 2;
            case DEPTH_ATTACHMENT -> 3;
            case TRANSFER_SRC -> 6;
            case TRANSFER_DST -> 7;
            case PRESENT -> 1000001002;
        };
    }
}
