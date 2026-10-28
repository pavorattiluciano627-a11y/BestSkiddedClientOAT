package dev.echo.client.events.render;

import dev.echo.client.events.Event;

/** Allows subscribers to suppress a block's culling face or culling shape. */
public class BlockCullingEvent extends Event {
    /** Creates a culling query with the default event phase. */
    public BlockCullingEvent() {
        super();
    }
}
