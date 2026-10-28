package dev.echo.client.events.lifecycle;

import dev.echo.client.events.Event;
import java.lang.invoke.MethodHandles;
import net.minecraft.client.gui.screens.Screen;

/**
 * Mutable next-screen payload retaining the original field-linkage initialization.
 * Direct targets have fixed/observed evidence; shared-cache applicability remains open.
 */
public class ScreenChangeEvent extends Event {
    private Screen screen;

    /**
     * Captures the requested screen after the genuine Event constructor.
     * @param screen requested screen, including null when closing a screen
     */
    public ScreenChangeEvent(Screen screen) {
        super();
        initializeScreenType();
        this.screen = screen;
    }

    /**
     * Returns the live payload after the original linkage's type initialization.
     * @return requested replacement screen, possibly null
     */
    public Screen getScreen() {
        initializeScreenType();
        return screen;
    }

    /**
     * Replaces only the payload after the original linkage's type initialization.
     * @param screen replacement screen, possibly null
     */
    public void setScreen(Screen screen) {
        initializeScreenType();
        this.screen = screen;
    }

    private static void initializeScreenType() {
        try {
            // Ordinary JVM class initialization, not reflected member dispatch.
            MethodHandles.lookup().ensureInitialized(Screen.class);
        } catch (IllegalAccessException inaccessibleType) {
            // Screen is public in the pinned unnamed-module Minecraft dependency.
            throw new IllegalAccessError(inaccessibleType.getMessage());
        }
    }
}
