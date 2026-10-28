package dev.echo.client.handlers;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.KeyPressedQueryEvent;
import dev.echo.client.events.lifecycle.WorldTickContextEvent;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.StickyKeyBinding;

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
        GameOptions options = CLIENT.options;
        if (options == null) {
            return;
        }
        if (options.sprintKey instanceof StickyKeyBinding && options.getSprintToggled().getValue()) {
            if (options.sprintKey.isPressed() != pressed) {
                options.sprintKey.setPressed(true);
            }
            return;
        }
        options.sprintKey.setPressed(pressed);
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
