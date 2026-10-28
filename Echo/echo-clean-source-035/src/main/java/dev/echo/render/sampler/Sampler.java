package dev.echo.render.sampler;

import dev.echo.render.resource.GpuResource;

/** Sampling state is independent of the texture and is owned as a GPU resource. */
public abstract class Sampler extends GpuResource {
    public final Filter filter;
    public final MipmapMode mipMode;
    public final WrapMode wrap;

    public Sampler(Filter filter, MipmapMode mipMode, WrapMode wrap) {
        this.filter = filter;
        this.mipMode = mipMode;
        this.wrap = wrap;
    }
}
