package dev.echo.client.events.lifecycle;

import dev.echo.client.events.Event;

/** Frame-start notification; all payload and cancellation state is inherited. */
public class FrameStartEvent extends Event {
    public FrameStartEvent() {
        super();
    }
}
