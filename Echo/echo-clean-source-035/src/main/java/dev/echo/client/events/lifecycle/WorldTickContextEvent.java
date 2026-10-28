package dev.echo.client.events.lifecycle;

import dev.echo.client.events.Event;

/** Published at the world-context tick hook and on joining a world. */
public class WorldTickContextEvent extends Event {
    public WorldTickContextEvent() {
        super();
    }
}
