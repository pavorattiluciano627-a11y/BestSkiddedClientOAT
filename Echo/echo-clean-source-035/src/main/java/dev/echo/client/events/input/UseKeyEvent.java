package dev.echo.client.events.input;

/** Click-count event for the use keybinding. */
public class UseKeyEvent extends KeybindingEvent {
    public UseKeyEvent(int clickCount) {
        super(clickCount);
    }
}
