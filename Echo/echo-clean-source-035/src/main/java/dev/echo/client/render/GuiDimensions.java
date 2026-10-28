package dev.echo.client.render;

import net.minecraft.client.MinecraftClient;

/** Echo's fixed 2x GUI coordinate system, independent of Minecraft's GUI scale. */
public final class GuiDimensions {
    public static final double SCALE = 2.0;

    private GuiDimensions() {}

    public static int width() {
        return (int) Math.ceil(MinecraftClient.getInstance().getWindow().getFramebufferWidth() / SCALE);
    }

    public static int height() {
        return (int) Math.ceil(MinecraftClient.getInstance().getWindow().getFramebufferHeight() / SCALE);
    }
}
