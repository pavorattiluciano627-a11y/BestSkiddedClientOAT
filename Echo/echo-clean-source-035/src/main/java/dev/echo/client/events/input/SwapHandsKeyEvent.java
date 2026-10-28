package dev.echo.client.events.input;

/** Click-count event for the swap-hands keybinding. */
public class SwapHandsKeyEvent extends KeybindingEvent {
    public SwapHandsKeyEvent(int clickCount) {
        super(clickCount);
    }
}
