package dev.echo.client.render;

import net.minecraft.client.Minecraft;

/** Echo's fixed 2x GUI coordinate system, independent of Minecraft's GUI scale. */
public final class GuiDimensions {
    public static final double SCALE = 2.0;

    private GuiDimensions() {}

    public static int width() {
        return (int) Math.ceil(Minecraft.getInstance().getWindow().getWidth() / SCALE);
    }

    public static int height() {
        return (int) Math.ceil(Minecraft.getInstance().getWindow().getHeight() / SCALE);
    }
}
