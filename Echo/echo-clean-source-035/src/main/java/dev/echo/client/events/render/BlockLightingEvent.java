package dev.echo.client.events.render;

import dev.echo.client.events.Event;

/** Allows subscribers to request emissive lighting and full ambient-occlusion light. */
public class BlockLightingEvent extends Event {
    /**
     * Requests emissive lighting or an ambient-occlusion light level of {@code 1.0F}.
     * Each query starts false; this is independent of the inherited cancellation flag.
     */
    public boolean fullBrightness;
}
