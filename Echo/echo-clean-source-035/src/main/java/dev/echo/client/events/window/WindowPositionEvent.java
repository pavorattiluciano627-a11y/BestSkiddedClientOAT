package dev.echo.client.events.window;

import dev.echo.client.events.Event;

/** The window's new position, as received by its native position callback. */
public class WindowPositionEvent extends Event {
    /** Horizontal window coordinate. */
    public final int x;
    /** Vertical window coordinate. */
    public final int y;

    /**
     * Captures the callback coordinates without scaling or clamping.
     * @param x horizontal coordinate
     * @param y vertical coordinate
     */
    public WindowPositionEvent(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
