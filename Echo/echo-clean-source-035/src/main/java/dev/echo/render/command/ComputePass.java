package dev.echo.render.command;

import dev.echo.render.buffer.BufferSlice;
import dev.echo.render.pipeline.ComputePipeline;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.texture.TextureAccess;
import dev.echo.render.texture.TextureView;
import dev.echo.render.util.RenderChecks;

/** Backend-neutral compute resource validation and command operations. */
public abstract class ComputePass {
    public abstract void setPipeline(ComputePipeline pipeline);

    public void setBuffer(int binding, BufferSlice slice) {
        RenderChecks.require((slice.buffer().usage & 4) == 0,
                "buffer must not be a staging buffer");
    }

    public void setTexture(int binding, TextureView view) {
        RenderChecks.require(view.access == TextureAccess.SAMPLED
                        || view.access == TextureAccess.STORAGE_READ
                        || view.access == TextureAccess.STORAGE_WRITE,
                "texture bound at {} must be in a SAMPLED or STORAGE state", binding);
    }

    public abstract void setSampler(int binding, Sampler sampler);
    public abstract void setPushConstants(long address, int bytes);
    public abstract void dispatch(int x, int y, int z);
    public abstract void end();
}
