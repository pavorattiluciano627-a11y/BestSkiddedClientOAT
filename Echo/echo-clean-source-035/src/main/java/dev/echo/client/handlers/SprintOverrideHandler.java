package dev.echo.client.handlers;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.KeyPressedQueryEvent;
import dev.echo.client.events.lifecycle.WorldTickContextEvent;
import net.minecraft.client.Options;
import net.minecraft.client.ToggleKeyMapping;

/** Applies per-tick requests to force or suppress sprinting, including toggle-sprint keys. */
public class SprintOverrideHandler extends ClientHandler {
    private static boolean forceSprint;
    private static boolean suppressSprint;

    public static void setForceSprint(boolean force) {
        forceSprint = force;
        if (!force || CLIENT.player == null || CLIENT.player.isSprinting()) {
            return;
        }
        CLIENT.player.setSprinting(true);
        setSprintKeyPressed(true);
    }

    public static void setSuppressSprint(boolean suppress) {
        suppressSprint = suppress;
        if (!suppress || CLIENT.player == null || !CLIENT.player.isSprinting()) {
            return;
        }
        CLIENT.player.setSprinting(false);
        setSprintKeyPressed(false);
    }

    private static void setSprintKeyPressed(boolean pressed) {
        Options options = CLIENT.options;
        if (options == null) {
            return;
        }
        if (options.keySprint instanceof ToggleKeyMapping && options.toggleSprint().get()) {
            if (options.keySprint.isDown() != pressed) {
                options.keySprint.setDown(true);
            }
            return;
        }
        options.keySprint.setDown(pressed);
    }

    public static void resetOverrides() {
        forceSprint = false;
        suppressSprint = false;
    }

    @Subscribe
    public void onWorldTickContext(WorldTickContextEvent event) {
        resetOverrides();
    }

    @Subscribe
    public void onSprintKeyQuery(KeyPressedQueryEvent event) {
        if (suppressSprint) {
            event.setPressed(false);
            return;
        }
        if (forceSprint) {
            event.setPressed(true);
        }
    }

    public static boolean isForceSprint() {
        return forceSprint;
    }

    public static boolean isSuppressSprint() {
        return suppressSprint;
    }
}
