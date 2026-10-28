package dev.echo.render.effect;

import dev.echo.client.render.BackdropMode;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.RenderPass;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.pipeline.GraphicsPipelineInfo;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.sampler.SamplerInfo;
import dev.echo.render.texture.Texture;
import dev.echo.render.texture.TextureAccess;
import dev.echo.render.texture.TextureFormat;
import dev.echo.render.texture.TextureInfo;
import dev.echo.render.texture.TextureLayout;
import dev.echo.render.texture.TextureView;
import java.nio.FloatBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * The original down/up WGSL backdrop passes. Each level owns a separate BGRA8
 * texture; the Minecraft input is borrowed. Framebuffer wrapping is a caller
 * responsibility, so this stage has no hidden access to Minecraft global state.
 */
public final class RenderPassBackdropEffect implements BackdropPass {
    private final GraphicsPipeline downsample;
    private final GraphicsPipeline upsample;
    private final Sampler sampler;
    private final int maximumPasses;
    private Texture[] textures;
    private TextureView[] views;
    private final RenderDevice device;

    public RenderPassBackdropEffect(RenderDevice device, int maximumPasses, int width, int height) {
        this.device = device;
        this.maximumPasses = maximumPasses;
        GraphicsPipelineInfo downInfo = new GraphicsPipelineInfo(device, "shaders/wgsl/down.wgsl");
        GraphicsPipelineInfo upInfo = new GraphicsPipelineInfo(device, "shaders/wgsl/up.wgsl");
        downInfo.blendEnabled = false;
        downInfo.cullEnabled = false;
        downInfo.depthTestEnabled = false;
        downInfo.pushConstantBytes = 8;
        upInfo.blendEnabled = false;
        upInfo.cullEnabled = false;
        upInfo.depthTestEnabled = false;
        upInfo.pushConstantBytes = 4;
        downsample = device.createGraphicsPipeline(downInfo);
        upsample = device.createGraphicsPipeline(upInfo);
        sampler = device.createSampler(SamplerInfo.builder().build());
        allocateLevels(width, height);
    }

    private void allocateLevels(int width, int height) {
        textures = new Texture[maximumPasses + 1];
        views = new TextureView[maximumPasses + 1];
        for (int level = 0; level <= maximumPasses; level++) {
            textures[level] = device.createTexture(TextureInfo.builder()
                    .width(Math.max(1, width >> level)).height(Math.max(1, height >> level))
                    .format(TextureFormat.BGRA8Unorm).usage(9).mipCount(1).build());
            views[level] = textures[level].views[0];
        }
    }

    private void releaseLevels() {
        for (Texture texture : textures) {
            device.release(texture);
        }
    }

    @Override
    public BackdropTextures render(CommandEncoder commands, TextureView input,
                                    boolean sourceYUp, float radius, BackdropMode mode) {
        int width = input.width(0);
        int height = input.height(0);
        if (textures[0].width != width || textures[0].height != height) {
            releaseLevels();
            allocateLevels(width, height);
        }

        commands.transitionTexture(input, 2, TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
        commands.transitionTexture(views[1], 2, TextureAccess.COLOR_ATTACHMENT, TextureLayout.COLOR_ATTACHMENT);
        RenderPass pass = commands.beginRenderPass(views[1], OptionalInt.empty(), null, OptionalDouble.empty());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer constants = stack.callocFloat(2);
            long address = MemoryUtil.memAddress(constants);
            float scaledRadius = radius * mode.radiusMultiplier();
            pass.setPipeline(downsample);
            pass.setSampler(1, sampler);
            int downPasses = mode.downsamplePasses(maximumPasses);
            for (int level = 0; level < downPasses; level++) {
                int targetLevel = level + 1;
                TextureView source = level == 0 ? input : views[level];
                TextureView target = views[targetLevel];
                if (level > 0) {
                    commands.transitionTexture(source, 2, TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
                    commands.transitionTexture(target, 2, TextureAccess.COLOR_ATTACHMENT, TextureLayout.COLOR_ATTACHMENT);
                    pass.setAttachments(target, null);
                }
                pass.setTexture(0, source);
                pass.setViewport(0, 0, textures[targetLevel].width, textures[targetLevel].height);
                constants.put(0, (level + 0.5F) * scaledRadius);
                constants.put(1, level == 0 && sourceYUp ? 1.0F : 0.0F);
                pass.setPushConstants(address, 8);
                pass.draw(4, 1, 0);
            }
            if (mode.upsamplePasses(maximumPasses) > 0) {
                pass.setPipeline(upsample);
                pass.setSampler(1, sampler);
                int retainedLevel = mode.retainedMipLevel(maximumPasses);
                for (int level = downPasses - 1; level >= retainedLevel; level--) {
                    TextureView source = views[level + 1];
                    TextureView target = views[level];
                    commands.transitionTexture(source, 2, TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
                    commands.transitionTexture(target, 2, TextureAccess.COLOR_ATTACHMENT, TextureLayout.COLOR_ATTACHMENT);
                    pass.setAttachments(target, null);
                    pass.setViewport(0, 0, textures[level].width, textures[level].height);
                    pass.setTexture(0, source);
                    constants.put(0, (level + 0.5F) * scaledRadius);
                    pass.setPushConstants(address, 4);
                    pass.draw(4, 1, 0);
                }
            }
        }
        // Original closes the stack before ending the render pass; failure in a
        // draw does not call end(), and this ordering is intentionally retained.
        pass.end();
        TextureView result = views[mode.retainedMipLevel(maximumPasses)];
        commands.transitionTexture(result, 2, TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
        return mode == BackdropMode.FULL
                ? new BackdropTextures(views[1], views[0])
                : new BackdropTextures(result, result);
    }

    @Override
    public void close() {
        downsample.destroy();
        upsample.destroy();
        device.release(sampler);
        releaseLevels();
    }
}
