package dev.echo.client.events.lifecycle;

import dev.echo.client.events.Event;

/** Notification emitted by the Minecraft client tick hook. */
public class ClientTickEvent extends Event {
    /** Creates a tick notification with the default event phase. */
    public ClientTickEvent() {
        super();
    }
}
