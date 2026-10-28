package dev.echo.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;

/** Creation, attachment clearing, and color blitting for Minecraft framebuffers. */
public final class FramebufferOperations {
    private FramebufferOperations() {
    }

    /** Creates a framebuffer with the requested dimensions and depth attachment. */
    public static RenderTarget create(String name, int width, int height, boolean useDepth) {
        return new TextureTarget(name, width, height, useDepth, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM);
    }

    /** Clears color to transparent black and depth to its far-plane value. */
    public static void clear(CommandEncoder encoder, RenderTarget framebuffer) {
        encoder.clearColorAndDepthTextures(
                framebuffer.getColorTexture(), new org.joml.Vector4f(0.0F, 0.0F, 0.0F, 0.0F),
                framebuffer.getDepthTexture(), 1.0);
    }

    /** Blits the source framebuffer into the destination's color attachment. */
    public static void blitColor(RenderTarget source, RenderTarget destination) {
        source.blitAndBlendToTexture(source.getColorTextureView(), destination.getColorTextureView());
    }
}
