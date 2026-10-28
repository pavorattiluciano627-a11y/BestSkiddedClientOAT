package dev.echo.client.events.input;

import dev.echo.client.events.Event;

/** Keyboard callback payload. Analysis candidate: field linkage awaits key proof. */
public class KeyboardEvent extends Event {
    private int key;
    private int action;

    /** Captures the key code followed by the native callback action. */
    public KeyboardEvent(int key, int action) {
        this.key = key;
        this.action = action;
    }

    /** Returns the key code; it is not a scan code. */
    public int getKey() { return key; }

    /** Returns the callback action without normalizing press/release/repeat. */
    public int getAction() { return action; }
}
