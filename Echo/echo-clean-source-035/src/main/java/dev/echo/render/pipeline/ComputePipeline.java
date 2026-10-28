package dev.echo.render.pipeline;

import dev.echo.render.resource.GpuResource;

/** Compute program and its allocation descriptor. Destruction is backend-specific. */
public abstract class ComputePipeline extends GpuResource {
    public final ComputePipelineInfo info;

    public ComputePipeline(ComputePipelineInfo info) {
        this.info = info;
    }

    @Override
    public abstract void destroy();
}
