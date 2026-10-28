package dev.echo.client.integration;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

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
        return window.getFramebufferWidth() + "x" + window.getFramebufferHeight();
    }

    /**
     * Formats the window dimensions.
     * @param window window being reported
     * @return width and height separated by {@code x}
     */
    public static String windowSize(Window window) {
        return window.getWidth() + "x" + window.getHeight();
    }

    /**
     * Identifies the concrete class loader used for Minecraft.
     * @return fully qualified class-loader implementation name
     */
    public static String minecraftClassLoader() {
        return MinecraftClient.class.getClassLoader().getClass().getName();
    }

    /**
     * Formats the window's current scale factor.
     * @param window window being reported
     * @return decimal scale factor
     */
    public static String scaleFactor(Window window) {
        return String.valueOf(window.getScaleFactor());
    }

    /**
     * Formats the configured fullscreen option.
     * @param client client whose options are reported
     * @return option value as text
     */
    public static String fullscreen(MinecraftClient client) {
        return String.valueOf(client.options.getFullscreen().getValue());
    }

    /**
     * Formats the configured vertical synchronization option.
     * @param client client whose options are reported
     * @return option value as text
     */
    public static String verticalSync(MinecraftClient client) {
        return String.valueOf(client.options.getEnableVsync().getValue());
    }

    /**
     * Formats the configured maximum frame rate.
     * @param client client whose options are reported
     * @return option value as text
     */
    public static String maximumFps(MinecraftClient client) {
        return String.valueOf(client.options.getMaxFps().getValue());
    }

    /**
     * Formats the configured view distance.
     * @param client client whose options are reported
     * @return option value as text
     */
    public static String viewDistance(MinecraftClient client) {
        return String.valueOf(client.options.getViewDistance().getValue());
    }

    /**
     * Formats the configured graphics preset.
     * @param client client whose options are reported
     * @return option value as text
     */
    public static String graphicsPreset(MinecraftClient client) {
        return String.valueOf(client.options.getPreset().getValue());
    }
}
