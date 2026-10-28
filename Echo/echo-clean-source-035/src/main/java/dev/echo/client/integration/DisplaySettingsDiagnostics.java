package dev.echo.client.integration;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

/** Formats the display settings included in the client's diagnostic report. */
public final class DisplaySettingsDiagnostics {
    private DisplaySettingsDiagnostics() {
    }

    /**
     * Formats the framebuffer dimensions.
     * @param window window being reported
     * @return width and height separated by {@code x}
     */
    public static String framebufferSize(Window window) {
        return window.getWidth() + "x" + window.getHeight();
    }

    /**
     * Formats the window dimensions.
     * @param window window being reported
     * @return width and height separated by {@code x}
     */
    public static String windowSize(Window window) {
        return window.getScreenWidth() + "x" + window.getScreenHeight();
    }

    /**
     * Identifies the concrete class loader used for Minecraft.
     * @return fully qualified class-loader implementation name
     */
    public static String minecraftClassLoader() {
        return Minecraft.class.getClassLoader().getClass().getName();
    }

    /**
     * Formats the window's current scale factor.
     * @param window window being reported
     * @return decimal scale factor
     */
    public static String scaleFactor(Window window) {
        return String.valueOf(window.getGuiScale());
    }

    /**
     * Formats the configured fullscreen option.
     * @param client client whose options are reported
     * @return option value as text
     */
    public static String fullscreen(Minecraft client) {
        return String.valueOf(client.options.fullscreen().get());
    }

    /**
     * Formats the configured vertical synchronization option.
     * @param client client whose options are reported
     * @return option value as text
     */
    public static String verticalSync(Minecraft client) {
        return String.valueOf(client.options.enableVsync().get());
    }

    /**
     * Formats the configured maximum frame rate.
     * @param client client whose options are reported
     * @return option value as text
     */
    public static String maximumFps(Minecraft client) {
        return String.valueOf(client.options.framerateLimit().get());
    }

    /**
     * Formats the configured view distance.
     * @param client client whose options are reported
     * @return option value as text
     */
    public static String viewDistance(Minecraft client) {
        return String.valueOf(client.options.renderDistance().get());
    }

    /**
     * Formats the configured graphics preset.
     * @param client client whose options are reported
     * @return option value as text
     */
    public static String graphicsPreset(Minecraft client) {
        return String.valueOf(client.options.graphicsPreset().get());
    }
}
