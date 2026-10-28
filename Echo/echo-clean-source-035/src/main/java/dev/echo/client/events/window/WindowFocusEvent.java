package dev.echo.client.events.window;

import dev.echo.client.events.Event;

/** The window's new focus state from its native focus callback. */
public class WindowFocusEvent extends Event {
    /** Whether the window gained focus. */
    public final boolean focused;

    /**
     * Captures the callback focus state.
     * @param focused whether the window has focus
     */
    public WindowFocusEvent(boolean focused) {
        this.focused = focused;
    }
}
