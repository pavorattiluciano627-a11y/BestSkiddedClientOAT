package dev.echo.render.effect;

import dev.echo.client.render.BackdropMode;
import dev.echo.client.render.math.RenderMath;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.ComputePass;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.pipeline.ComputePipeline;
import dev.echo.render.pipeline.ComputePipelineInfo;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.sampler.SamplerInfo;
import dev.echo.render.texture.Texture;
import dev.echo.render.texture.TextureAccess;
import dev.echo.render.texture.TextureFormat;
import dev.echo.render.texture.TextureInfo;
import dev.echo.render.texture.TextureLayout;
import dev.echo.render.texture.TextureView;
import java.nio.FloatBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** Compute down/up backdrop over individual views of one RGBA8 mipmapped texture. */
public final class ComputeBackdropEffect implements BackdropPass {
    private final ComputePipeline downsample;
    private final ComputePipeline upsample;
    private final Sampler sampler;
    private final int maximumPasses;
    private Texture mipTexture;
    private final RenderDevice device;

    public ComputeBackdropEffect(RenderDevice device, int maximumPasses) {
        this.device = device;
        this.maximumPasses = maximumPasses;
        ComputePipelineInfo downInfo = new ComputePipelineInfo(device, "shaders/wgsl/compute/down.wgsl");
        ComputePipelineInfo upInfo = new ComputePipelineInfo(device, "shaders/wgsl/compute/up.wgsl");
        downInfo.pushConstantBytes = 8;
        upInfo.pushConstantBytes = 4;
        downsample = device.createComputePipeline(downInfo);
        upsample = device.createComputePipeline(upInfo);
        sampler = device.createSampler(SamplerInfo.builder().build());
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        allocateMipTexture(framebuffer.textureWidth, framebuffer.textureHeight);
    }

    private void allocateMipTexture(int width, int height) {
        mipTexture = device.createTexture(TextureInfo.builder().width(width).height(height)
                .format(TextureFormat.RGBA8Unorm).usage(3).mipCount(maximumPasses + 1).build());
    }

    @Override
    public BackdropTextures render(CommandEncoder commands, TextureView input,
                                    boolean sourceYUp, float radius, BackdropMode mode) {
        int width = input.width(0);
        int height = input.height(0);
        if (mipTexture.width != width || mipTexture.height != height) {
            device.release(mipTexture);
            allocateMipTexture(width, height);
        }
        commands.transitionTexture(input, 4, TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
        commands.transitionTexture(mipTexture.views[1], 4, TextureAccess.STORAGE_WRITE, TextureLayout.GENERAL);
        ComputePass pass = commands.beginComputePass();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer constants = stack.callocFloat(2);
            long address = MemoryUtil.memAddress(constants);
            float scaledRadius = radius * mode.radiusMultiplier();
            pass.setPipeline(downsample);
            pass.setSampler(1, sampler);
            int downPasses = mode.downsamplePasses(maximumPasses);
            for (int level = 0; level < downPasses; level++) {
                int targetLevel = level + 1;
                int targetWidth = Math.max(1, mipTexture.width >> targetLevel);
                int targetHeight = Math.max(1, mipTexture.height >> targetLevel);
                TextureView source = level == 0 ? input : mipTexture.views[level];
                TextureView target = mipTexture.views[targetLevel];
                if (level > 0) {
                    commands.transitionTexture(source, 4, TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
                    commands.transitionTexture(target, 4, TextureAccess.STORAGE_WRITE, TextureLayout.GENERAL);
                }
                pass.setTexture(0, source);
                pass.setTexture(2, target);
                constants.put(0, (level + 0.5F) * scaledRadius);
                constants.put(1, level == 0 && sourceYUp ? 1.0F : 0.0F);
                pass.setPushConstants(address, 8);
                pass.dispatch(RenderMath.workgroupsForExtent(targetWidth), RenderMath.workgroupsForExtent(targetHeight), 1);
            }
            if (mode.upsamplePasses(maximumPasses) > 0) {
                pass.setPipeline(upsample);
                pass.setSampler(1, sampler);
                int retainedLevel = mode.retainedMipLevel(maximumPasses);
                for (int level = downPasses - 1; level >= retainedLevel; level--) {
                    int targetWidth = Math.max(1, mipTexture.width >> level);
                    int targetHeight = Math.max(1, mipTexture.height >> level);
                    TextureView source = mipTexture.views[level + 1];
                    TextureView target = mipTexture.views[level];
                    // The final full-resolution pass also exposes its source to fragment reads.
                    commands.transitionTexture(source, level == 0 ? 6 : 4,
                            TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
                    commands.transitionTexture(target, 4, TextureAccess.STORAGE_WRITE, TextureLayout.GENERAL);
                    pass.setTexture(0, source);
                    pass.setTexture(2, target);
                    constants.put(0, (level + 0.5F) * scaledRadius);
                    pass.setPushConstants(address, 4);
                    pass.dispatch(RenderMath.workgroupsForExtent(targetWidth), RenderMath.workgroupsForExtent(targetHeight), 1);
                }
            }
        }
        pass.end();
        TextureView result = mipTexture.views[mode.retainedMipLevel(maximumPasses)];
        commands.transitionTexture(result, 2, TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
        return mode == BackdropMode.FULL
                ? new BackdropTextures(mipTexture.views[1], mipTexture.views[0])
                : new BackdropTextures(result, result);
    }

    @Override
    public void close() {
        downsample.destroy();
        upsample.destroy();
        device.release(sampler);
        device.release(mipTexture);
    }
}
