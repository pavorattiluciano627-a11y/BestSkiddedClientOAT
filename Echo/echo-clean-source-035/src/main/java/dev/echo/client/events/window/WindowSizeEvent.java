package dev.echo.client.events.window;

import dev.echo.client.events.Event;

/** Reports the new logical window size supplied by the window-size callback. */
public class WindowSizeEvent extends Event {
    /** New logical width; this is not the framebuffer width. */
    public final int width;
    /** New logical height; this is not the framebuffer height. */
    public final int height;

    /**
     * Records the callback dimensions without clamping them.
     * @param width new logical window width
     * @param height new logical window height
     */
    public WindowSizeEvent(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
