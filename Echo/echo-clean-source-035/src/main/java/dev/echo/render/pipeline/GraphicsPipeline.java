package dev.echo.render.pipeline;

import dev.echo.render.resource.GpuResource;

/** Graphics program and the mutable descriptor retained by its backend. */
public abstract class GraphicsPipeline extends GpuResource {
    public final GraphicsPipelineInfo info;

    public GraphicsPipeline(GraphicsPipelineInfo info) {
        this.info = info;
    }

    @Override
    public abstract void destroy();
}
