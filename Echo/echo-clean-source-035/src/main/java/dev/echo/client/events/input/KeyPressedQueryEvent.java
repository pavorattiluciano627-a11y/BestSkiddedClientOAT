package dev.echo.client.events.input;

import dev.echo.client.events.Event;

/** Queries the sprint key's pressed state, distinguishing an override from cancellation. */
public class KeyPressedQueryEvent extends Event {
    private boolean pressed;
    private boolean overridden;

    /**
     * Starts a query with the key's stored pressed value and no override.
     * @param pressed stored keybinding state
     */
    public KeyPressedQueryEvent(boolean pressed) {
        this.pressed = pressed;
        this.overridden = false;
    }

    /** @return the current pressed value */
    public boolean isPressed() {
        return pressed;
    }

    /**
     * Overrides the queried value, even when it equals the initial value.
     * @param pressed value the key query should return
     */
    public void setPressed(boolean pressed) {
        this.pressed = pressed;
        this.overridden = true;
    }

    /** @return whether a subscriber explicitly supplied a pressed value */
    public boolean isOverridden() {
        return overridden;
    }
}
