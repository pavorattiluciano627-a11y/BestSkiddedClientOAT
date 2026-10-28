package dev.echo.client.events.render;

import dev.echo.client.events.Event;

/** Cancellation suppresses the spyglass overlay at its producer hook. */
public class SpyglassOverlayEvent extends Event {
    public SpyglassOverlayEvent() {
        super();
    }
}
