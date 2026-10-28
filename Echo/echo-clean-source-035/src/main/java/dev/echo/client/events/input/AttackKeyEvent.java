package dev.echo.client.events.input;

/** Click-count event for the attack keybinding. */
public class AttackKeyEvent extends KeybindingEvent {
    public AttackKeyEvent(int clickCount) {
        super(clickCount);
    }
}
