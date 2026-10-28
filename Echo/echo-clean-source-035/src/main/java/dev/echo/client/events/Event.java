package dev.echo.client.events;

/**
 * Shared mutable cancellation state and immutable lifecycle phase.
 * Analysis-only while the accepted joint proof still has supporting roots.
 * The original counter is retained here rather than assumed to stay zero.
 */
public abstract class Event {
    public final EventPhase phase;
    public boolean cancelled;
    private static int eventCounter;

    static {
        if (getEventCounter() != 0) {
            setEventCounter(9);
        }
    }

    protected Event(EventPhase phase) {
        this.phase = phase;
        this.cancelled = false;
    }

    protected Event() {
        this(EventPhase.NONE);
    }

    /** Dispatches this same instance and returns its final cancellation state. */
    public boolean post() {
        return EventBus.post(this);
    }

    public final boolean isEarly() {
        return phase == EventPhase.EARLY;
    }

    public final boolean isPre() {
        return phase == EventPhase.PRE;
    }

    public final boolean isPost() {
        return phase == EventPhase.POST;
    }

    public final void cancel() {
        cancelled = true;
    }

    /** Original counter write, retained only in this unpromoted analysis type. */
    public static void setEventCounter(int value) {
        eventCounter = value;
    }

    public static int getEventCounter() {
        return eventCounter;
    }

    public static int getEventCounterFallbackValue() {
        return getEventCounter() == 0 ? 19 : 0;
    }
}
