package dev.echo.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.echo.client.Echo;
import dev.echo.client.setting.ColorSetting;
import dev.echo.render.FramebufferOperations;
import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferInfo;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.RenderPass;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.pipeline.GraphicsPipelineInfo;
import dev.echo.render.sampler.Filter;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.sampler.SamplerInfo;
import dev.echo.render.texture.Texture;
import dev.echo.render.texture.TextureAccess;
import dev.echo.render.texture.TextureFormat;
import dev.echo.render.texture.TextureInfo;
import dev.echo.render.texture.TextureLayout;
import dev.echo.render.texture.TextureView;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** Shared gradient, separable glow blur and composite passes for entity and hand materials. */
public final class TexturePasses {
    private static final int UNIFORM_BYTES = 64;
    private static GraphicsPipeline gradientPipeline;
    private static GraphicsPipeline blurPipeline;
    private static GraphicsPipeline compositePipeline;
    private static Sampler sampler;
    private static Buffer uniforms;
    private static final BorrowedTextureViewCache framebufferImport = new BorrowedTextureViewCache();
    private static final BorrowedTextureViewCache previewImport = new BorrowedTextureViewCache();
    private static TexturePassResources framebufferTargets;
    private static TexturePassResources previewTargets;
    private static Texture previewOutput;

    private TexturePasses() {
        super();
    }

    /** Applies the material in place to the captured framebuffer's color attachment. */
    public static void applyMaterial(EntityMaterialSettings material, RenderTarget target) {
        RenderDevice device = Echo.renderDevice;
        if (device == null || Echo.commandEncoder == null) {
            return;
        }
        TextureView color = framebufferImport.get(device, target.getColorTextureView());
        framebufferTargets = ensureTargets(device, framebufferTargets, target.width, target.height);
        renderMaterial(device, material, color, color, framebufferTargets, 0);
    }

    /** Renders a preview into the effect-owned texture, retaining the Minecraft source as borrowed. */
    public static TextureView renderToTexture(EntityMaterialSettings material, GpuTextureView source,
                                               int width, int height, int overrideArgb) {
        RenderDevice device = Echo.renderDevice;
        if (device == null || Echo.commandEncoder == null) {
            return null;
        }
        TextureView input = previewImport.get(device, source);
        previewTargets = ensureTargets(device, previewTargets, width, height);
        if (previewOutput != null && (previewOutput.width != width || previewOutput.height != height)) {
            device.release(previewOutput);
            previewOutput = null;
        }
        if (previewOutput == null) {
            previewOutput = createTarget(device, width, height);
        }
        renderMaterial(device, material, input, previewOutput.views[0], previewTargets, overrideArgb);
        return previewOutput.views[0];
    }

    private static void renderMaterial(RenderDevice device, EntityMaterialSettings material,
                                       TextureView input, TextureView output,
                                       TexturePassResources targets, int overrideArgb) {
        ensureResources(device);
        writeUniforms(material, overrideArgb);
        // The original reads the current encoder after material getters have run.
        CommandEncoder commands = Echo.commandEncoder;
        TextureView outline = targets.outline().views[0];
        TextureView horizontal = targets.horizontalBlur().views[0];
        TextureView vertical = targets.verticalBlur().views[0];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long push = MemoryUtil.memAddress(stack.callocFloat(2));
            sampled(commands, input);
            colorAttachment(commands, outline);
            RenderPass pass = commands.beginRenderPass(outline, OptionalInt.of(0), null, OptionalDouble.empty());
            device.setViewportToTexture(pass, outline);
            pass.setPipeline(gradientPipeline);
            pass.setBuffer(0, uniforms.wholeSlice());
            pass.setTexture(1, input);
            pass.setSampler(2, sampler);
            NativeMemoryWrites.putFloat(push, input.yFlipped ? 1.0F : 0.0F);
            pass.setPushConstants(push, 4);
            pass.draw(4, 1, 0);

            blur(device, pass, commands, outline, horizontal, push, 1.0F, 0.0F);
            blur(device, pass, commands, horizontal, vertical, push, 0.0F, 1.0F);
            sampled(commands, outline);
            sampled(commands, vertical);
            colorAttachment(commands, output);
            pass.setAttachments(output, null);
            device.setViewportToTexture(pass, output);
            pass.setPipeline(compositePipeline);
            pass.setBuffer(0, uniforms.wholeSlice());
            pass.setTexture(1, outline);
            pass.setTexture(2, vertical);
            pass.setSampler(3, sampler);
            pass.draw(4, 1, 0);
            pass.end();
        }
    }

    private static void blur(RenderDevice device, RenderPass pass, CommandEncoder commands,
                              TextureView input, TextureView output, long push, float x, float y) {
        sampled(commands, input);
        colorAttachment(commands, output);
        pass.setAttachments(output, null);
        device.setViewportToTexture(pass, output);
        pass.setPipeline(blurPipeline);
        pass.setBuffer(0, uniforms.wholeSlice());
        pass.setTexture(1, input);
        pass.setSampler(2, sampler);
        NativeMemoryWrites.putFloat2(push, x, y);
        pass.setPushConstants(push, 8);
        pass.draw(4, 1, 0);
    }

    private static void sampled(CommandEncoder commands, TextureView view) {
        commands.transitionTexture(view, 2, TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
    }

    private static void colorAttachment(CommandEncoder commands, TextureView view) {
        commands.transitionTexture(view, 2, TextureAccess.COLOR_ATTACHMENT, TextureLayout.COLOR_ATTACHMENT);
    }

    public static RenderTarget createFramebuffer(String name, int width, int height, boolean useDepth) {
        return FramebufferOperations.create(name, width, height, useDepth);
    }

    /** Original processListener is a color blit, despite the old consumer name clearFramebuffer. */
    public static void blitToMainFramebuffer(RenderTarget source) {
        FramebufferOperations.blitColor(source, Minecraft.getInstance().gameRenderer.mainRenderTarget());
    }

    public static void releaseResources() {
        RenderDevice device = Echo.renderDevice;
        if (device == null) {
            return;
        }
        if (framebufferTargets != null) {
            framebufferTargets.release(device);
            framebufferTargets = null;
        }
        if (previewTargets != null) {
            previewTargets.release(device);
            previewTargets = null;
        }
        if (previewOutput != null) {
            device.release(previewOutput);
            previewOutput = null;
        }
        if (uniforms != null) {
            device.release(uniforms);
            device.release(sampler);
            device.release(gradientPipeline);
            device.release(blurPipeline);
            device.release(compositePipeline);
            uniforms = null;
            sampler = null;
            gradientPipeline = null;
            blurPipeline = null;
            compositePipeline = null;
        }
    }

    private static void ensureResources(RenderDevice device) {
        if (uniforms != null) {
            return;
        }
        gradientPipeline = device.createGraphicsPipeline(pipelineInfo(device, "gradient", 4));
        blurPipeline = device.createGraphicsPipeline(pipelineInfo(device, "blur", 8));
        compositePipeline = device.createGraphicsPipeline(pipelineInfo(device, "composite", 0));
        sampler = device.createSampler(SamplerInfo.builder().filter(Filter.NEAREST).build());
        uniforms = device.createBuffer(BufferInfo.builder().usage(2).size(UNIFORM_BYTES)
                .mapped(true).dynamic(true).build());
    }

    private static GraphicsPipelineInfo pipelineInfo(RenderDevice device, String name, int pushBytes) {
        GraphicsPipelineInfo info = new GraphicsPipelineInfo(device, "shaders/wgsl/chams/" + name + ".wgsl");
        info.blendEnabled = false;
        info.cullEnabled = false;
        info.depthTestEnabled = false;
        info.depthWriteEnabled = false;
        info.pushConstantBytes = pushBytes;
        return info;
    }

    private static TexturePassResources ensureTargets(RenderDevice device, TexturePassResources current, int width, int height) {
        if (current != null && current.outline().width == width && current.outline().height == height) {
            return current;
        }
        if (current != null) {
            current.release(device);
        }
        return new TexturePassResources(createTarget(device, width, height),
                createTarget(device, width, height), createTarget(device, width, height));
    }

    private static Texture createTarget(RenderDevice device, int width, int height) {
        return device.createTexture(TextureInfo.builder().width(width).height(height)
                .format(TextureFormat.BGRA8Unorm).usage(9).build());
    }

    private static void writeUniforms(EntityMaterialSettings material, int overrideArgb) {
        boolean override = overrideArgb != 0 && material.usesTeamColors();
        long address = uniforms.mappedAddress();
        NativeMemoryWrites.putFloat(address, material.getLineWidthSetting().getValue());
        NativeMemoryWrites.putFloat(address + 4L, material.getGradientScaleSetting().getValue());
        NativeMemoryWrites.putFloat(address + 8L, material.getFillOpacitySetting().getValue());
        NativeMemoryWrites.putFloat(address + 12L, material.getOutlineOpacitySetting().getValue());
        NativeMemoryWrites.putFloat(address + 16L, material.usesTeamColors() && !override ? 1.0F : 0.0F);
        NativeMemoryWrites.putFloat(address + 20L, material.getAnimationPhase());
        NativeMemoryWrites.putFloat(address + 24L, material.getGlowRadiusSetting().getValue());
        NativeMemoryWrites.putFloat(address + 28L, material.getGlowIntensitySetting().getValue());
        if (override) {
            writeArgb(address + 32L, overrideArgb);
            writeArgb(address + 48L, overrideArgb);
        } else {
            writeColor(address + 32L, material.getColor1Setting());
            writeColor(address + 48L, material.getColor2Setting());
        }
    }

    private static void writeColor(long address, ColorSetting color) {
        NativeMemoryWrites.putFloat4(address, color.getRed() / 255.0F, color.getGreen() / 255.0F,
                color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
    }

    private static void writeArgb(long address, int argb) {
        NativeMemoryWrites.putFloat4(address, ARGB.red(argb) / 255.0F,
                ARGB.green(argb) / 255.0F, ARGB.blue(argb) / 255.0F,
                ARGB.alpha(argb) / 255.0F);
    }
}
