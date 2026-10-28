package dev.echo.client.scripting.api;

import dev.echo.client.events.Event;

/** Cancellation API for the retained event instance. */
public class EventView {
    protected final Event event;

    /** Retains the supplied event, without adding null rejection. */
    public EventView(Event event) { this.event = event; }

    /** Cancels the same event instance being dispatched. */
    public void cancel() { event.cancel(); }

    /** Reads the event's live mutable cancellation field. */
    public boolean isCancelled() { return event.cancelled; }
}
