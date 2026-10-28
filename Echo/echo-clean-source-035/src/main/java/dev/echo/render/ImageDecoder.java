package dev.echo.render;

import dev.echo.client.resources.ClientResources;
import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferInfo;
import dev.echo.render.command.TransferPass;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.texture.Texture;
import dev.echo.render.texture.TextureFormat;
import dev.echo.render.texture.TextureInfo;
import dev.echo.render.texture.TextureView;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * Image loading, RGBA decoding, and upload through the renderer's transfer API.
 *
 * <p>Analysis candidate for the recorded negative-short / enabled-cleanup domain.
 * Caller, shared-cache, and original control-state obligations remain in the
 * provider receipt; this class does not implement a runtime key selector.
 */
public final class ImageDecoder {
    private ImageDecoder() {
    }

    public static TextureView loadTexture(RenderDevice device, String path) {
        byte[] encoded = ClientResources.readBytes(path);
        if (encoded == null) {
            throw new IllegalArgumentException("Image not found: " + path);
        }
        return decodeTexture(device, encoded);
    }

    public static TextureView decodeTexture(RenderDevice device, byte[] bytes) {
        initializeFontDefinitions();
        ByteBuffer encoded = MemoryUtil.memAlloc(bytes.length);
        try {
            encoded.put(bytes).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);
                ByteBuffer pixels = STBImage.stbi_load_from_memory(encoded, width, height, channels, 4);
                if (pixels == null) {
                    throw new IllegalStateException("Failed to decode image: " + STBImage.stbi_failure_reason());
                }
                try {
                    return uploadRgba(device, pixels, width.get(0), height.get(0));
                } finally {
                    STBImage.stbi_image_free(pixels);
                }
            }
        } finally {
            MemoryUtil.memFree(encoded);
        }
    }

    /**
     * Uploads the requested byte region without changing the source position.
     * Transfer completion precedes staging-buffer release. Failure cleanup remains
     * with the same owner as in the original operation.
     */
    public static TextureView uploadRgba(RenderDevice device, ByteBuffer pixels, int width, int height) {
        Texture texture = device.createTexture(TextureInfo.builder()
                .width(width).height(height).format(TextureFormat.RGBA8Unorm).usage(5).build());
        long byteCount = (long) width * height * 4L;
        Buffer staging = device.createBuffer(BufferInfo.builder()
                .usage(4).size(byteCount).mapped(true).build());
        MemoryUtil.memCopy(MemoryUtil.memAddress(pixels), staging.mappedAddress(), byteCount);
        TransferPass transfer = device.beginTransfer();
        transfer.uploadTexture(texture.views[0], staging.wholeSlice());
        transfer.end();
        device.release(staging);
        return texture.views[0];
    }

    /** Preserve the original control-reader's class-initialization boundary. */
    private static void initializeFontDefinitions() {
        try {
            Class.forName(FontAsset.class.getName(), true, FontAsset.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }
}
