package dev.echo.render;

import com.mojang.blaze3d.systems.CommandEncoder;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;

/** Creation, attachment clearing, and color blitting for Minecraft framebuffers. */
public final class FramebufferOperations {
    private FramebufferOperations() {
    }

    /** Creates a framebuffer with the requested dimensions and depth attachment. */
    public static Framebuffer create(String name, int width, int height, boolean useDepth) {
        return new SimpleFramebuffer(name, width, height, useDepth);
    }

    /** Clears color to transparent black and depth to its far-plane value. */
    public static void clear(CommandEncoder encoder, Framebuffer framebuffer) {
        encoder.clearColorAndDepthTextures(
                framebuffer.getColorAttachment(), 0,
                framebuffer.getDepthAttachment(), 1.0);
    }

    /** Blits the source framebuffer into the destination's color attachment. */
    public static void blitColor(Framebuffer source, Framebuffer destination) {
        source.drawBlit(destination.getColorAttachmentView());
    }
}
