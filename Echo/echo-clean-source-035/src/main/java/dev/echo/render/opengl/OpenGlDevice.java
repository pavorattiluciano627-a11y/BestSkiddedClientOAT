package dev.echo.render.opengl;

import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferInfo;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.TransferPass;
import dev.echo.render.device.DeviceCapabilities;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.pipeline.ComputePipeline;
import dev.echo.render.pipeline.ComputePipelineInfo;
import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.pipeline.GraphicsPipelineInfo;
import dev.echo.render.queue.CommandQueue;
import dev.echo.render.queue.QueueInfo;
import dev.echo.render.resource.ResourceLoader;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.sampler.SamplerInfo;
import dev.echo.render.surface.Surface;
import dev.echo.render.surface.SurfaceInfo;
import dev.echo.render.texture.Texture;
import dev.echo.render.texture.TextureFormat;
import dev.echo.render.texture.TextureInfo;
import dev.echo.render.texture.TextureView;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL30C.*;

/** Device backed by the host's current OpenGL context and framebuffer state tracker. */
public class OpenGlDevice extends RenderDevice {
    public final GlFramebufferAccess framebuffers;
    public final int glslVersion;
    public final boolean baseInstanceSupported;

    public OpenGlDevice(GlFramebufferAccess framebuffers, ResourceLoader resources, DeviceCapabilities capabilities) {
        super(resources, capabilities);
        this.framebuffers = framebuffers;
        glslVersion = glGetInteger(GL_MAJOR_VERSION) * 100 + glGetInteger(GL_MINOR_VERSION) * 10;
        baseInstanceSupported = GL.getCapabilities().glDrawArraysInstancedBaseInstance != 0;
    }

    @Override public CommandEncoder commandEncoder() { return new OpenGlCommandEncoder(this); }
    @Override public TransferPass beginTransfer() { return new OpenGlTransferPass(); }
    @Override public GraphicsPipeline createGraphicsPipeline(GraphicsPipelineInfo info) { return new OpenGlGraphicsPipeline(this, info); }
    @Override public ComputePipeline createComputePipeline(ComputePipelineInfo info) { return new OpenGlComputePipeline(this, info); }

    @Override
    public Buffer createBuffer(BufferInfo info) {
        return new OpenGlBuffer(info.usage, info.size, info.dynamic, info.mapped, info.explicitFlush);
    }

    @Override
    public Texture createTexture(TextureInfo info) {
        if ((info.usage & 2) != 0 && !capabilities().storageImages()) {
            throw new IllegalStateException("Device does not support shader write textures (requires GL 4.2)");
        }
        return new OpenGlTexture(info.width, info.height, info.depth, info.format, info.usage, info.mipCount);
    }

    @Override
    public Sampler createSampler(SamplerInfo info) {
        return new OpenGlSampler(info.filter, info.mipMode, info.wrap);
    }

    public TextureView importTextureView(int textureId, int width, int height, TextureFormat format, int internalFormat) {
        return OpenGlTexture.importTexture(width, height, format, textureId, internalFormat).views[0];
    }

    public static DeviceCapabilities queryCapabilities(boolean reversedDepth) {
        int version = glGetInteger(GL_MAJOR_VERSION) * 100 + glGetInteger(GL_MINOR_VERSION) * 10;
        boolean gl43 = version >= 430;
        return new DeviceCapabilities(gl43, gl43, false, version >= 420, reversedDepth);
    }

    @Override public boolean isVulkan() { return false; }
    @Override public TextureFormat depthFormat() { return OpenGlTextureFormats.fromInternalFormat(framebuffers.depthInternalFormat()); }

    @Override
    public CommandQueue createQueue(QueueInfo info) {
        int supported = OpenGlQueue.supportedCapabilities(this);
        if ((supported & info.capabilities) != info.capabilities) {
            throw new IllegalStateException("OpenGL cannot provide a queue with the requested capabilities: " + info.capabilities);
        }
        return new OpenGlQueue(this, supported);
    }

    @Override public Surface createSurface(SurfaceInfo info) { return new OpenGlSurface(info); }
}
