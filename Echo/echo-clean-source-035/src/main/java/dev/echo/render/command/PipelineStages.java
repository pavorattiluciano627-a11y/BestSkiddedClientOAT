package dev.echo.render.command;

/** Renderer stage-mask bits; these are not the numeric Vulkan stage flags. */
public final class PipelineStages {
    public static final int TOP_OF_PIPE = 16;
    public static final int VERTEX_SHADER = 1;
    public static final int FRAGMENT_SHADER = 2;
    public static final int COMPUTE_SHADER = 4;
    public static final int ALL_GRAPHICS = 8;

    private PipelineStages() {
    }
}
