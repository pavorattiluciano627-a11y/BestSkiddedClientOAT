package dev.echo.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;

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

    public static GpuTextureView playerSkin(Player player) {
        boolean alternate = getRenderControlFlag();
        if (!alternate && !(player instanceof AbstractClientPlayer)) {
            return null;
        }
        // The alternate branch still performs the original cast before returning null.
        AbstractClientPlayer clientPlayer = (AbstractClientPlayer) player;
        if (alternate) {
            return null;
        }
        var texture = Minecraft.getInstance().getTextureManager()
                .getTexture(clientPlayer.getSkin().body().texturePath());
        return texture == null ? null : texture.getTextureView();
    }

    public static Screen getScreen() {
        return Minecraft.getInstance().gui.screen();
    }

    public static void setScreen(Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }

    public static Overlay getOverlay() {
        return Minecraft.getInstance().gui.overlay();
    }

    public static boolean isHudHidden() {
        return Minecraft.getInstance().gui.hud.isHidden();
    }

    public static boolean hasOverlay() {
        return getOverlay() != null;
    }

    public static RenderTarget getFramebuffer(Minecraft client) {
        return client.gameRenderer.mainRenderTarget();
    }

    public static Camera getCamera(Minecraft client) {
        return client.gameRenderer.mainCamera();
    }

    public static boolean isConnectedToLocalServer(Minecraft client) {
        return client.hasSingleplayerServer();
    }

    public static void reloadWorldRenderer(Minecraft client) {
        boolean alternate = getRenderControlFlag();
        if (!alternate && client.level == null) {
            return;
        }
        client.levelRenderer.resetLevelRenderData();
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
