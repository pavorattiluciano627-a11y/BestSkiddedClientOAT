package dev.echo.render.opengl;

import dev.echo.render.buffer.BufferAccess;
import dev.echo.render.texture.TextureAccess;

/** Consumer barrier bits recovered from the original exhaustive enum switches. */
public final class OpenGlMemoryBarriers {
    private OpenGlMemoryBarriers() {}

    public static int bufferConsumerBits(BufferAccess access) {
        return switch (access) {
            case UNDEFINED -> 0;
            case VERTEX_READ -> 0x0001; // GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT
            case INDEX_READ -> 0x0002; // GL_ELEMENT_ARRAY_BARRIER_BIT
            case UNIFORM_READ -> 0x0004; // GL_UNIFORM_BARRIER_BIT
            case INDIRECT_READ -> 0x0040; // GL_COMMAND_BARRIER_BIT
            case STORAGE_READ, STORAGE_WRITE -> 0x2000; // GL_SHADER_STORAGE_BARRIER_BIT
            case TRANSFER_READ, TRANSFER_WRITE -> 0x0200; // GL_BUFFER_UPDATE_BARRIER_BIT
        };
    }

    public static int textureConsumerBits(TextureAccess access) {
        return switch (access) {
            case UNDEFINED, PRESENT -> 0;
            case SAMPLED -> 0x0008; // GL_TEXTURE_FETCH_BARRIER_BIT
            case STORAGE_READ, STORAGE_WRITE -> 0x0020; // GL_SHADER_IMAGE_ACCESS_BARRIER_BIT
            case COLOR_ATTACHMENT, DEPTH_ATTACHMENT -> 0x0400; // GL_FRAMEBUFFER_BARRIER_BIT
            case TRANSFER_READ, TRANSFER_WRITE -> 0x0100; // GL_TEXTURE_UPDATE_BARRIER_BIT
        };
    }
}
