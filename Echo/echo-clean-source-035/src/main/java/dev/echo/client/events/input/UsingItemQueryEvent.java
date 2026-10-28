package dev.echo.client.events.input;

import dev.echo.client.events.Event;

/** Mutable item-use query. Analysis candidate: all field linkage awaits key proof. */
public class UsingItemQueryEvent extends Event {
    private boolean usingItem;

    /** Captures the original item-use query result. */
    public UsingItemQueryEvent(boolean usingItem) {
        this.usingItem = usingItem;
    }

    /** Returns the item-use result supplied to input handling. */
    public boolean isUsingItem() { return usingItem; }

    /** Replaces the query result without changing cancellation. */
    public void setUsingItem(boolean usingItem) { this.usingItem = usingItem; }
}
