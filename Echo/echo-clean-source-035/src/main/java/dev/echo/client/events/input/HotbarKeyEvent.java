package dev.echo.client.events.input;

/** Click-count event for a hotbar keybinding, carrying its zero-based slot. */
public class HotbarKeyEvent extends KeybindingEvent {
    public final int slot;

    public HotbarKeyEvent(int slot, int clickCount) {
        super(clickCount);
        this.slot = slot;
    }
}
