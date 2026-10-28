package dev.echo.render.pipeline;

import dev.echo.render.device.RenderDevice;
import dev.echo.render.shader.naga.Module;
import dev.echo.render.shader.wgsl.WgslParser;

/**
 * Conditional constructor reconstruction. The resource/Naga signatures and the
 * original constructor's guard side effects must be resolved before promotion.
 */
public final class ComputePipelineInfo {
    public final Module module;
    public int pushConstantBytes;

    public ComputePipelineInfo(RenderDevice device, String path) {
        pushConstantBytes = 0;
        module = WgslParser.parseSource(device.resourceLoader.loadText(path));
    }
}
