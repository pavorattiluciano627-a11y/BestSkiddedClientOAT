package dev.echo.client.screens;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.setting.Setting;
import net.minecraft.client.Minecraft;

/**
 * Owns the reusable dashboard screen and the setting currently receiving UI input.
 *
 * <p>This is a functional source candidate. The original synthetic guard/counter
 * tail is tracked separately in the open counter-v3 proof; its removal is not
 * presented as an unconditional whole-program equivalence claim.
 */
public class ScreenManager implements MinecraftAccess {
    /** The original shared setting-focus field, also used by setting widgets. */
    public static Setting focusedSetting = null;

    private DashboardScreen dashboardScreen;

    /** Constructs the dashboard at the original service-initialization boundary. */
    public void initialize() {
        dashboardScreen = new DashboardScreen();
    }

    /** Opens the stored dashboard when the client and its renderer are available. */
    public void open() {
        if (Echo.eventsDisabled || Echo.uiRenderer == null) {
            Echo.LOGGER.warn("[echo] ClickGUI open refused: eventsDisabled={} uiRenderer={} "
                    + "(the renderer is built by the resource-reload listener; if this "
                    + "repeats, that listener never ran)",
                    Echo.eventsDisabled, Echo.uiRenderer != null);
            if (Echo.rendererFailure != null) {
                Echo.LOGGER.error("[echo] renderer is unavailable because startup failed earlier: {}",
                        Echo.rendererFailure.toString());
            }
            return;
        }
        Minecraft.getInstance().gui.setScreen(dashboardScreen);
    }

    /**
     * Tests the original screen identity comparison, including equality of two nulls.
     *
     * @return whether the current screen is the stored dashboard reference
     */
    public boolean isOpen() {
        return Minecraft.getInstance().gui.screen() == dashboardScreen;
    }

    /** Clears setting focus before the normal Minecraft screen-removal callback. */
    public void close() {
        focusedSetting = null;
        Minecraft.getInstance().gui.setScreen(null);
    }

    /** Closes the current screen, then releases the reusable dashboard reference. */
    public void destroy() {
        close();
        dashboardScreen = null;
    }
}
