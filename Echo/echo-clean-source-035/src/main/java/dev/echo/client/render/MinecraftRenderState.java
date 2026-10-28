package dev.echo.client.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Original Minecraft render/query operations and shared control state.
 * This is conditional analysis source: key/caller, class-initialization and shared
 * member-cache applicability remain explicit in the accompanying receipt.
 */
public final class MinecraftRenderState {
    // Original ConfigurationStore.activeFeature; this is not the HUD-hidden option.
    private static boolean renderControlFlag;

    static {
        if (getRenderControlFlag()) {
            setRenderControlFlag(true);
        }
    }

    private MinecraftRenderState() {}

    public static GpuTextureView playerSkin(PlayerEntity player) {
        boolean alternate = getRenderControlFlag();
        if (!alternate && !(player instanceof AbstractClientPlayerEntity)) {
            return null;
        }
        // The alternate branch still performs the original cast before returning null.
        AbstractClientPlayerEntity clientPlayer = (AbstractClientPlayerEntity) player;
        if (alternate) {
            return null;
        }
        var texture = MinecraftClient.getInstance().getTextureManager()
                .getTexture(clientPlayer.getSkin().body().texturePath());
        return texture == null ? null : texture.getGlTextureView();
    }

    public static Screen getScreen() {
        return MinecraftClient.getInstance().currentScreen;
    }

    public static void setScreen(Screen screen) {
        MinecraftClient.getInstance().setScreen(screen);
    }

    public static Overlay getOverlay() {
        return MinecraftClient.getInstance().getOverlay();
    }

    public static boolean isHudHidden() {
        return MinecraftClient.getInstance().options.hudHidden;
    }

    public static boolean hasOverlay() {
        return getOverlay() != null;
    }

    public static Framebuffer getFramebuffer(MinecraftClient client) {
        return client.getFramebuffer();
    }

    public static Camera getCamera(MinecraftClient client) {
        return client.gameRenderer.getCamera();
    }

    public static boolean isConnectedToLocalServer(MinecraftClient client) {
        return client.isConnectedToLocalServer();
    }

    public static void reloadWorldRenderer(MinecraftClient client) {
        boolean alternate = getRenderControlFlag();
        if (!alternate && client.world == null) {
            return;
        }
        client.worldRenderer.reload();
    }

    public static void setRenderControlFlag(boolean value) {
        renderControlFlag = value;
    }

    public static boolean getRenderControlFlag() {
        return renderControlFlag;
    }

    public static boolean isRenderControlFlagClear() {
        return !getRenderControlFlag();
    }
}
