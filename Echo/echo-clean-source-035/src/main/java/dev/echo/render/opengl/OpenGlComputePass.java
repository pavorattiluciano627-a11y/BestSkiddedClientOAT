package dev.echo.render.opengl;

import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferSlice;
import dev.echo.render.command.ComputePass;
import dev.echo.render.pipeline.ComputePipeline;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.shader.ResourceBindingKind;
import dev.echo.render.texture.TextureView;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL42C;

/** OpenGL compute pass preserving the host's GL state on completion. */
public class OpenGlComputePass extends ComputePass {
    private final OpenGlStateSnapshot savedState;
    private OpenGlComputePipeline pipeline;

    public OpenGlComputePass() { savedState = new OpenGlStateSnapshot(); }

    @Override public void setPipeline(ComputePipeline pipeline) {
        if (pipeline == this.pipeline) {
            return;
        }
        OpenGlComputePipeline glPipeline = (OpenGlComputePipeline) pipeline;
        glPipeline.bind();
        this.pipeline = glPipeline;
    }

    @Override public void setBuffer(int binding, BufferSlice slice) {
        Buffer resource = slice.buffer();
        if ((resource.usage & 4) != 0) {
            throw new IllegalStateException("Buffer can not be a staging buffer");
        }
        OpenGlBuffer buffer = (OpenGlBuffer) resource;
        GL15C.glBindBuffer(buffer.target, buffer.bufferId);
        buffer.uploadStagedData(buffer.target, slice.offset(), slice.length());
        GL31C.glBindBufferRange(buffer.target, binding, buffer.bufferId, slice.offset(), slice.length());
    }

    @Override public void setTexture(int binding, TextureView view) {
        super.setTexture(binding, view);
        OpenGlTextureView glView = (OpenGlTextureView) view;
        OpenGlTexture texture = (OpenGlTexture) glView.texture;
        if (pipeline.bindingKinds[binding] == ResourceBindingKind.STORAGE_IMAGE) {
            int access = switch (view.access) {
                case STORAGE_READ -> 35000;
                case STORAGE_WRITE -> 35001;
                default -> 35002;
            };
            GL42C.glBindImageTexture(binding, texture.textureId, glView.baseMipLevel,
                    texture.depth > 1, 0, access, texture.internalFormat);
            return;
        }
        int unit = pipeline.textureUnits[binding];
        GL15C.glActiveTexture(33984 + unit);
        GL15C.glBindTexture(texture.target, glView.textureId);
        if (!texture.imported && !glView.ownsTextureId()) {
            GL15C.glTexParameteri(texture.target, 33084, glView.baseMipLevel);
            GL15C.glTexParameteri(texture.target, 33085, glView.baseMipLevel + glView.mipCount - 1);
        }
    }

    @Override public void setSampler(int binding, Sampler sampler) {
        OpenGlSampler glSampler = (OpenGlSampler) sampler;
        int firstUnit = pipeline.samplerUnits[binding];
        int count = pipeline.unitCounts[binding];
        for (int index = 0; index < count; index++) {
            GL33C.glBindSampler(firstUnit + index, glSampler.samplerId);
        }
    }

    @Override public void setPushConstants(long address, int bytes) {
        OpenGlShaderCompiler.uploadPushConstants(pipeline.pushConstantUniforms, address);
    }

    @Override public void dispatch(int x, int y, int z) { pipeline.dispatch(x, y, z); }

    @Override public void end() { savedState.restore(); }
}
