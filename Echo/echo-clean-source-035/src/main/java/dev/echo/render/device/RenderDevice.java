package dev.echo.render.device;

import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferInfo;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.RenderPass;
import dev.echo.render.command.TransferPass;
import dev.echo.render.pipeline.ComputePipeline;
import dev.echo.render.pipeline.ComputePipelineInfo;
import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.pipeline.GraphicsPipelineInfo;
import dev.echo.render.queue.CommandQueue;
import dev.echo.render.queue.QueueInfo;
import dev.echo.render.resource.GpuResource;
import dev.echo.render.resource.ResourceLoader;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.sampler.SamplerInfo;
import dev.echo.render.surface.Surface;
import dev.echo.render.surface.SurfaceInfo;
import dev.echo.render.texture.Texture;
import dev.echo.render.texture.TextureFormat;
import dev.echo.render.texture.TextureInfo;
import dev.echo.render.texture.TextureView;
import org.lwjgl.system.MemoryUtil;

/**
 * Renderer resource factory and lifetime policy shared by the GL and Vulkan devices.
 * The host owns the native device/context. Individual resources are relinquished
 * through {@link #release(GpuResource)}, which Vulkan overrides with frame retirement.
 */
public abstract class RenderDevice {
    public final ResourceLoader resourceLoader;
    private final DeviceCapabilities capabilities;

    protected RenderDevice(ResourceLoader resourceLoader, DeviceCapabilities capabilities) {
        this.resourceLoader = resourceLoader;
        this.capabilities = capabilities;
    }

    /** Returns the capability object supplied by the actual backend probe. */
    public DeviceCapabilities capabilities() { return capabilities; }

    public void beginFrame() {
        // No frame-local queues in the base implementation; Vulkan overrides this operation.
    }

    public abstract CommandEncoder commandEncoder();
    public abstract TransferPass beginTransfer();
    public abstract GraphicsPipeline createGraphicsPipeline(GraphicsPipelineInfo info);
    public abstract ComputePipeline createComputePipeline(ComputePipelineInfo info);
    public abstract Buffer createBuffer(BufferInfo info);
    public abstract Texture createTexture(TextureInfo info);
    public abstract Sampler createSampler(SamplerInfo info);
    public abstract CommandQueue createQueue(QueueInfo info);
    public abstract Surface createSurface(SurfaceInfo info);
    public abstract boolean isVulkan();
    public abstract TextureFormat depthFormat();

    public void setViewportToTexture(RenderPass pass, TextureView view) {
        pass.setViewport(0, 0, view.width(0), view.height(0));
    }

    /** Relinquishes ownership using the backend's normal resource-retirement policy. */
    public void release(GpuResource resource) { resource.destroy(); }

    /** Bypasses deferred retirement; callers must ensure the resource is no longer in use. */
    public void destroyImmediately(GpuResource resource) { resource.destroy(); }

    /**
     * Replaces a mapped buffer, copies the requested prefix, then releases the old one.
     * Both lengths are real byte counts. The copied length is bounded by the new
     * allocation; the caller supplies a prefix backed by the source allocation.
     * Allocation/address/copy failures propagate before the old resource is released.
     * Normal release remains virtual, so Vulkan keeps the old allocation alive until
     * its frame slot is retired. Explicit-flush mode uses the builder's original default.
     */
    public Buffer reallocateMappedBuffer(Buffer source, long newSize, long copyBytes) {
        if (!source.mapped) {
            throw new UnsupportedOperationException("realloc requires a mapped buffer");
        }
        Buffer replacement = createBuffer(BufferInfo.builder().usage(source.usage).size(newSize)
                .dynamic(source.dynamic).mapped(true).build());
        // The artifact bounds against requested copy length and destination size, not source.size.
        MemoryUtil.memCopy(source.mappedAddress(), replacement.mappedAddress(), Math.min(copyBytes, newSize));
        release(source);
        return replacement;
    }
}
