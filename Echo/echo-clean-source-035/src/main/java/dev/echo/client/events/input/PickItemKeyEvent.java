package dev.echo.client.events.input;

/** Click-count event for the pick-item keybinding. */
public class PickItemKeyEvent extends KeybindingEvent {
    public PickItemKeyEvent(int clickCount) {
        super(clickCount);
    }
}
