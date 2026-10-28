package dev.echo.render.svg;

import dev.echo.client.render.SvgColorMode;
import dev.echo.client.render.SvgTextureKey;
import dev.echo.render.ImageDecoder;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.texture.TextureView;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

/**
 * Render-thread texture cache with a single daemon rasterization worker.
 * Rasterization never submits GPU work. A completed native pixel buffer is
 * uploaded by the next render-thread lookup and released on both upload exits.
 */
public final class SvgTextureCache implements AutoCloseable {
    private final RenderDevice device;
    private final Logger logger;
    private final SvgDocumentRasterizer documents = new SvgDocumentRasterizer();
    private final Map<SvgTextureKey, TextureView> textures = new HashMap<>();
    private final Map<SvgTextureKey, CompletableFuture<ByteBuffer>> pending = new HashMap<>();
    private final ExecutorService rasterizer = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().name("Echo-Svg-Raster").daemon(true).factory());

    /** Logger is the existing client logger supplied by the owning renderer. */
    public SvgTextureCache(RenderDevice device, Logger logger) {
        this.device = device;
        this.logger = logger;
    }

    /** Returns null while pixels are pending, or after a recorded rasterization failure. */
    public TextureView getOrSchedule(String resourcePath, int width, int height, SvgColorMode mode) {
        SvgTextureKey key = new SvgTextureKey(resourcePath, Math.max(1, width), Math.max(1, height), mode);
        TextureView texture = textures.get(key);
        if (texture != null) {
            return texture;
        }
        CompletableFuture<ByteBuffer> task = pending.computeIfAbsent(key, this::rasterizeAsync);
        if (!task.isDone() || task.isCompletedExceptionally()) {
            return null;
        }
        ByteBuffer pixels = task.join();
        pending.remove(key);
        try {
            texture = ImageDecoder.uploadRgba(device, pixels, key.width(), key.height());
            textures.put(key, texture);
            return texture;
        } finally {
            MemoryUtil.memFree(pixels);
        }
    }

    private CompletableFuture<ByteBuffer> rasterizeAsync(SvgTextureKey key) {
        return CompletableFuture.supplyAsync(() -> documents.rasterize(key), rasterizer)
                .whenComplete((pixels, failure) -> {
                    if (failure != null) {
                        logger.error("Failed to rasterize svg {}", key.resourcePath(), failure);
                    }
                });
    }

    /** Retains parsed SVG documents so another size can reuse them. */
    public void invalidateSized() {
        for (TextureView texture : textures.values()) {
            device.release(texture.texture);
        }
        textures.clear();
        pending.values().forEach(task -> task.thenAccept(MemoryUtil::memFree));
        pending.clear();
    }

    public int cachedTextureCount() {
        return textures.size();
    }

    @Override
    public void close() {
        rasterizer.shutdownNow();
        invalidateSized();
        documents.clearDocuments();
    }
}
