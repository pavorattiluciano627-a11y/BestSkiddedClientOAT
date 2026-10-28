package dev.echo.client.events.interaction;

import dev.echo.client.events.Event;
import dev.echo.client.events.EventPhase;

/** Lifecycle notification around client block-breaking input handling. */
public class BlockBreakingEvent extends Event {
    public BlockBreakingEvent(EventPhase phase) {
        super(phase);
    }

    public static class Pre extends BlockBreakingEvent {
        public Pre() {
            super(EventPhase.PRE);
        }
    }
}
