package dev.echo.client.events.input;

import dev.echo.client.events.Event;
import dev.echo.client.events.EventPhase;

/** Mouse deltas in the producer's cursor units, without scaling or normalization. */
public class MouseMovementEvent extends Event {
    public double deltaX;
    public double deltaY;

    protected MouseMovementEvent(EventPhase phase, double deltaX, double deltaY) {
        super(phase);
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    /** HEAD hook: the producer writes modified deltas back to the mouse. */
    public static class Pre extends MouseMovementEvent {
        public Pre(double deltaX, double deltaY) {
            super(EventPhase.PRE, deltaX, deltaY);
        }
    }

    /** TAIL hook: the producer checks cancellation but does not write deltas back. */
    public static class Post extends MouseMovementEvent {
        public Post(double deltaX, double deltaY) {
            super(EventPhase.POST, deltaX, deltaY);
        }
    }
}
