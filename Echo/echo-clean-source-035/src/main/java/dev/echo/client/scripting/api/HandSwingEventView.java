package dev.echo.client.scripting.api;

import dev.echo.client.events.interaction.HandSwingEvent;

/** Script-facing hand-swing player, enum name, and cancellation. */
public class HandSwingEventView extends EventView {
    private final HandSwingEvent swingEvent;

    /** Retains the original swing event. */
    public HandSwingEventView(HandSwingEvent event) {
        super(event);
        swingEvent = event;
    }

    /** Allocates a fresh wrapper without adding an attacker null check. */
    public PlayerView getPlayer() { return new PlayerView(swingEvent.getPlayer()); }

    /** Preserves the enum's uppercase MAIN_HAND/OFF_HAND spelling. */
    public String getHand() { return swingEvent.getHand().name(); }
}
