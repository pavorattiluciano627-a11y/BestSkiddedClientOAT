package dev.echo.render;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.echo.client.Echo;
import dev.echo.client.features.impl.render.HandChamsFeature;
import dev.echo.client.render.TexturePasses;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

/**
 * Captures uncolored hand outlines into an owned framebuffer, applies the active
 * hand material, and composites it into Minecraft's current color attachment.
 * Key/cache/guard applicability and the material-pass provider remain explicit
 * integration obligations in the adjacent hand-chams receipt.
 */
public class HandChamsRenderer {
    private static final int CAPTURE_OUTLINE_COLOR = -1;
    private static final String FRAMEBUFFER_NAME = "echo_hand_chams";

    private static boolean capturing;
    private static boolean flushingOutlines;
    private static boolean hasCapturedOutlines;
    private static Framebuffer framebuffer;

    /** The original public constructor only invokes Object's constructor. */
    public HandChamsRenderer() {
    }

    public static void beginCapture() {
        capturing = findEnabledFeature() != null;
    }

    public static void endCapture() {
        capturing = false;
    }

    /** Used by the world-renderer framebuffer redirect during outline drawing. */
    public static boolean isFlushingOutlines() {
        return flushingOutlines;
    }

    public static Framebuffer getFramebuffer() {
        return framebuffer;
    }

    public static int overrideOutlineColor(int outlineColor) {
        if (capturing && outlineColor == 0) {
            hasCapturedOutlines = true;
            return CAPTURE_OUTLINE_COLOR;
        }
        return outlineColor;
    }

    public static void flushCapturedOutlines() {
        if (!hasCapturedOutlines) {
            return;
        }
        hasCapturedOutlines = false;
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer mainFramebuffer = client.getFramebuffer();
        ensureFramebuffer(mainFramebuffer.textureWidth, mainFramebuffer.textureHeight);

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        FramebufferOperations.clear(encoder, framebuffer);
        flushingOutlines = true;
        try {
            client.getBufferBuilders().getOutlineVertexConsumers().draw();
        } finally {
            flushingOutlines = false;
        }

        HandChamsFeature material = findEnabledFeature();
        if (material != null) {
            TexturePasses.applyMaterial(material, framebuffer);
        }
        FramebufferOperations.blitColor(framebuffer, MinecraftClient.getInstance().getFramebuffer());
    }

    public static void reset() {
        capturing = false;
        hasCapturedOutlines = false;
        if (framebuffer != null) {
            framebuffer.delete();
            framebuffer = null;
        }
        TexturePasses.releaseResources();
    }

    private static void ensureFramebuffer(int width, int height) {
        if (framebuffer == null) {
            framebuffer = FramebufferOperations.create(FRAMEBUFFER_NAME, width, height, true);
        } else if (framebuffer.textureWidth != width || framebuffer.textureHeight != height) {
            framebuffer.resize(width, height);
        }
    }

    private static HandChamsFeature findEnabledFeature() {
        if (Echo.eventsDisabled || Echo.featureManager == null) {
            return null;
        }
        HandChamsFeature feature = (HandChamsFeature) Echo.featureManager.findFeature(HandChamsFeature.class);
        return feature != null && feature.isEnabled() ? feature : null;
    }
}
