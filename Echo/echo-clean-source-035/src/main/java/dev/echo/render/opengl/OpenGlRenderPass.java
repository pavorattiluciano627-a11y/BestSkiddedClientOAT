package dev.echo.render.opengl;

import dev.echo.render.buffer.BufferSlice;
import dev.echo.render.command.RenderPass;
import dev.echo.render.pipeline.BlendMode;
import dev.echo.render.pipeline.CullFace;
import dev.echo.render.pipeline.DepthCompare;
import dev.echo.render.pipeline.FrontFace;
import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.surface.RenderTargetLease;
import dev.echo.render.texture.TextureView;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.opengl.GL40C.GL_DRAW_INDIRECT_BUFFER;

/** Immediate rendering with scoped restoration of the host's OpenGL state. */
public class OpenGlRenderPass extends RenderPass {
    private final RenderTargetLease renderTargetLease;
    private final OpenGlStateSnapshot savedState;
    private final int framebufferId;
    private OpenGlGraphicsPipeline pipeline;
    private long indexByteOffset;
    private OpenGlBuffer vertexBuffer;
    private long vertexByteOffset;
    private boolean cullEnabled;
    private boolean depthTestEnabled;
    private boolean depthWriteEnabled;
    private boolean blendEnabled;
    private DepthCompare depthCompare;
    private BlendMode blendMode;
    private CullFace cullFace;
    private FrontFace frontFace;
    private final OpenGlDevice device;
    private float lineWidth;
    private boolean yFlipped;
    private int targetHeight;

    public OpenGlRenderPass(OpenGlDevice device, RenderTargetLease lease) {
        this.device = device;
        renderTargetLease = lease;
        framebufferId = lease == null ? glGenFramebuffers() : 0;
        savedState = new OpenGlStateSnapshot();
        cullEnabled = savedState.cullFaceEnabled;
        depthTestEnabled = savedState.depthTestEnabled;
        depthWriteEnabled = savedState.depthWriteEnabled;
        blendEnabled = savedState.blendEnabled;
        lineWidth = savedState.lineWidth;
        glColorMask(true, true, true, true);
        if (lease != null) {
            yFlipped = lease.isYFlipped();
            targetHeight = lease.height();
        }
    }

    @Override public boolean isYFlipped() { return yFlipped; }

    @Override
    public void setAttachments(TextureView color, TextureView depth) {
        if (framebufferId == 0) throw new IllegalStateException("setRenderTarget is not supported on an external render pass");
        yFlipped = color.yFlipped;
        targetHeight = color.height(0);
        frontFace = null;
        device.framebuffers.bindFramebuffer(GL_FRAMEBUFFER, framebufferId);
        OpenGlTexture colorTexture = (OpenGlTexture) color.texture;
        selectAttachmentMip(colorTexture, color.baseMipLevel);
        device.framebuffers.framebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D,
                colorTexture.textureId, color.baseMipLevel);
        device.framebuffers.framebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, 0, 0);
        device.framebuffers.framebufferTexture2D(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_TEXTURE_2D, 0, 0);
        if (depth != null) {
            OpenGlTexture depthTexture = (OpenGlTexture) depth.texture;
            int attachment = depthTexture.format.stencil ? GL_DEPTH_STENCIL_ATTACHMENT : GL_DEPTH_ATTACHMENT;
            selectAttachmentMip(depthTexture, depth.baseMipLevel);
            device.framebuffers.framebufferTexture2D(GL_FRAMEBUFFER, attachment, GL_TEXTURE_2D,
                    depthTexture.textureId, depth.baseMipLevel);
        }
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) throw new IllegalStateException("Incomplete framebuffer: " + status);
    }

    private static void selectAttachmentMip(OpenGlTexture texture, int mip) {
        if (texture.imported || texture.immutableStorage) return;
        int previous = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, texture.textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, mip);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, mip);
        glBindTexture(GL_TEXTURE_2D, previous);
    }

    void clearAttachments(OptionalInt color, OptionalDouble depth) {
        glDisable(GL_SCISSOR_TEST);
        if (color.isPresent()) {
            int argb = color.getAsInt();
            glClearColor((argb >> 16 & 255) / 255.0f, (argb >> 8 & 255) / 255.0f,
                    (argb & 255) / 255.0f, (argb >> 24 & 255) / 255.0f);
            glClear(GL_COLOR_BUFFER_BIT);
        }
        if (depth.isPresent()) {
            setDepthWriteEnabled(true);
            glClearDepth(depth.getAsDouble());
            glClear(GL_DEPTH_BUFFER_BIT);
        }
    }

    public void setDepthCompare(DepthCompare compare) {
        if (depthCompare == compare) return;
        glDepthFunc(switch (compare) {
            case LESS_OR_EQUAL -> GL_LEQUAL;
            case GREATER_OR_EQUAL -> GL_GEQUAL;
        });
        depthCompare = compare;
    }

    public void setBlendMode(BlendMode mode) {
        if (blendMode == mode) return;
        switch (mode) {
            case SRC_ALPHA -> glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            case PREMULTIPLIED_ALPHA -> glBlendFuncSeparate(GL_ONE, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            case ADDITIVE -> glBlendFuncSeparate(GL_ONE, GL_ONE, GL_ONE, GL_ONE);
        }
        blendMode = mode;
    }

    public void setCullFace(CullFace face) {
        if (cullFace == face) return;
        glCullFace(switch (face) { case FRONT -> GL_FRONT; case BACK -> GL_BACK; });
        cullFace = face;
    }

    public void setLineWidth(float width) {
        if (lineWidth == width) return;
        glLineWidth(width);
        lineWidth = width;
    }

    public void setFrontFace(FrontFace face) {
        if (frontFace == face) return;
        glFrontFace(switch (face) {
            case CLOCKWISE -> yFlipped ? GL_CW : GL_CCW;
            case COUNTER_CLOCKWISE -> yFlipped ? GL_CCW : GL_CW;
        });
        frontFace = face;
    }

    public void setCullEnabled(boolean enabled) {
        if (cullEnabled == enabled) return;
        setCapability(GL_CULL_FACE, enabled);
        cullEnabled = enabled;
    }

    public void setBlendEnabled(boolean enabled) {
        if (blendEnabled == enabled) return;
        setCapability(GL_BLEND, enabled);
        blendEnabled = enabled;
    }

    public void setDepthTestEnabled(boolean enabled) {
        if (depthTestEnabled == enabled) return;
        setCapability(GL_DEPTH_TEST, enabled);
        depthTestEnabled = enabled;
    }

    public void setDepthWriteEnabled(boolean enabled) {
        if (depthWriteEnabled == enabled) return;
        glDepthMask(enabled);
        depthWriteEnabled = enabled;
    }

    private void setCapability(int capability, boolean enabled) {
        if (enabled) glEnable(capability);
        else glDisable(capability);
    }

    @Override
    public void setPipeline(GraphicsPipeline pipeline) {
        if (pipeline == this.pipeline) return;
        OpenGlGraphicsPipeline nativePipeline = (OpenGlGraphicsPipeline) pipeline;
        nativePipeline.bind(this);
        this.pipeline = nativePipeline;
    }

    @Override
    public void setScissor(int x, int y, int width, int height) {
        glEnable(GL_SCISSOR_TEST);
        glScissor(x, yFlipped ? targetHeight - (y + height) : y, width, height);
    }

    @Override
    public void setVertexBuffer(BufferSlice slice) {
        super.setVertexBuffer(slice);
        if ((slice.buffer().usage & 1) == 0) throw new IllegalStateException("Buffer must be a vertex buffer");
        OpenGlBuffer buffer = (OpenGlBuffer) slice.buffer();
        glBindBuffer(buffer.target, buffer.bufferId);
        buffer.uploadStagedData(buffer.target, slice.offset(), slice.length());
        pipeline.setVertexBufferPointer(slice.offset());
        vertexBuffer = buffer;
        vertexByteOffset = slice.offset();
    }

    @Override
    public void setIndexBuffer(BufferSlice slice) {
        super.setIndexBuffer(slice);
        OpenGlBuffer buffer = (OpenGlBuffer) slice.buffer();
        glBindBuffer(buffer.target, buffer.bufferId);
        buffer.uploadStagedData(buffer.target, slice.offset(), slice.length());
        indexByteOffset = slice.offset();
    }

    @Override
    public void setBuffer(int binding, BufferSlice slice) {
        if ((slice.buffer().usage & 4) != 0) throw new IllegalStateException("Buffer can not be a staging buffer");
        OpenGlBuffer buffer = (OpenGlBuffer) slice.buffer();
        glBindBuffer(buffer.target, buffer.bufferId);
        buffer.uploadStagedData(buffer.target, slice.offset(), slice.length());
        glBindBufferRange(buffer.target, binding, buffer.bufferId, slice.offset(), slice.length());
    }

    @Override
    public void setTexture(int binding, TextureView view) {
        super.setTexture(binding, view);
        bindTexture(pipeline.textureUnits[binding], (OpenGlTextureView) view);
    }

    private static void bindTexture(int unit, OpenGlTextureView view) {
        OpenGlTexture texture = (OpenGlTexture) view.texture;
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(texture.target, view.textureId);
        if (texture.imported || view.ownsTextureId()) return;
        glTexParameteri(texture.target, GL_TEXTURE_BASE_LEVEL, view.baseMipLevel);
        glTexParameteri(texture.target, GL_TEXTURE_MAX_LEVEL, view.baseMipLevel + view.mipCount - 1);
    }

    @Override
    public void setTextures(int binding, TextureView[] views) {
        super.setTextures(binding, views);
        int unit = pipeline.textureUnits[binding];
        for (int index = 0; index < views.length; index++) bindTexture(unit + index, (OpenGlTextureView) views[index]);
    }

    @Override
    public void setSampler(int binding, Sampler sampler) {
        OpenGlSampler nativeSampler = (OpenGlSampler) sampler;
        int firstUnit = pipeline.samplerUnits[binding];
        int count = pipeline.unitCounts[binding];
        for (int index = 0; index < count; index++) glBindSampler(firstUnit + index, nativeSampler.samplerId);
    }

    @Override
    public void setPushConstants(long address, int bytes) {
        OpenGlShaderCompiler.uploadPushConstants(pipeline.pushConstantUniforms, address);
    }

    @Override
    public void drawInstanced(int vertices, int instances, int firstVertex, int firstInstance) {
        if (firstInstance == 0 || !pipeline.isInstanced() || device.baseInstanceSupported) {
            pipeline.draw(vertices, instances, firstVertex, firstInstance);
            return;
        }
        glBindBuffer(vertexBuffer.target, vertexBuffer.bufferId);
        pipeline.setVertexBufferPointer(vertexByteOffset + (long) firstInstance * pipeline.vertexStride());
        pipeline.draw(vertices, instances, firstVertex, 0);
        pipeline.setVertexBufferPointer(vertexByteOffset);
    }

    @Override
    public void drawIndexed(int indices, int instances, int firstIndex) {
        pipeline.drawIndexed(indices, instances, indexByteOffset + (long) firstIndex * 4);
    }

    @Override
    public void drawIndirect(BufferSlice commands, int drawCount, int stride) {
        super.drawIndirect(commands, drawCount, stride);
        if ((commands.buffer().usage & 8) == 0) throw new IllegalStateException("Buffer must be an indirect buffer");
        OpenGlBuffer buffer = (OpenGlBuffer) commands.buffer();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, buffer.bufferId);
        buffer.uploadStagedData(GL_DRAW_INDIRECT_BUFFER, commands.offset(), commands.length());
        pipeline.drawIndirect(commands.offset(), drawCount, stride);
    }

    @Override
    public void setViewport(int x, int y, int width, int height) {
        glViewport(x, yFlipped ? targetHeight - (y + height) : y, width, height);
    }

    @Override
    public void end() {
        savedState.restore();
        if (framebufferId != 0) {
            device.framebuffers.bindFramebuffer(GL_FRAMEBUFFER, savedState.drawFramebuffer);
            device.framebuffers.deleteFramebuffer(framebufferId);
        }
        if (renderTargetLease != null) renderTargetLease.close();
    }
}
