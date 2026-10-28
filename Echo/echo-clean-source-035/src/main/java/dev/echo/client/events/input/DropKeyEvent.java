package dev.echo.client.events.input;

/** Click-count event for the drop keybinding. */
public class DropKeyEvent extends KeybindingEvent {
    public DropKeyEvent(int clickCount) {
        super(clickCount);
    }
}
