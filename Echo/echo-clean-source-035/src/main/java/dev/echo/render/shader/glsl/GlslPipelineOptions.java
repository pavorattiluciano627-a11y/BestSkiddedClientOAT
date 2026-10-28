package dev.echo.render.shader.glsl;

import dev.echo.render.shader.ShaderStage;

/** Stage and entry point are supplied by the pipeline compiler. */
public final class GlslPipelineOptions {
    public ShaderStage stage;
    public String entryPoint;
}
