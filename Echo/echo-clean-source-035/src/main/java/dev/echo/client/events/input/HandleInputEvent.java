package dev.echo.client.events.input;

import dev.echo.client.events.Event;
import dev.echo.client.events.EventPhase;

/** Lifecycle markers surrounding the input handler. */
public class HandleInputEvent extends Event {
    public HandleInputEvent(EventPhase phase) {
        super(phase);
    }

    /** The before-input marker carries EARLY, not PRE. */
    public static class Pre extends HandleInputEvent {
        public Pre() {
            super(EventPhase.EARLY);
        }
    }

    /** Marker constructed by the input handler's return hook. */
    public static class Post extends HandleInputEvent {
        public Post() {
            super(EventPhase.POST);
        }
    }
}
