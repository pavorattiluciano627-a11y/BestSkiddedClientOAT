package dev.echo.client.events.input;

import dev.echo.client.events.Event;

/** Reports a selected-slot change before the inventory applies it. */
public class SelectedSlotEvent extends Event {
    private int previousSlot;
    private int selectedSlot;

    /**
     * Records the previous and requested slot values without range checks.
     * @param previousSlot inventory's selected slot before the call
     * @param selectedSlot slot requested by the call
     */
    public SelectedSlotEvent(int previousSlot, int selectedSlot) {
        this.previousSlot = previousSlot;
        this.selectedSlot = selectedSlot;
    }

    /** @return the event's previous slot value */
    public int getPreviousSlot() {
        return previousSlot;
    }

    /** @return the event's requested slot value */
    public int getSelectedSlot() {
        return selectedSlot;
    }

    /**
     * Changes the value observed by subsequent subscribers.
     * @param previousSlot replacement previous-slot value
     */
    public void setPreviousSlot(int previousSlot) {
        this.previousSlot = previousSlot;
    }

    /**
     * Changes the value observed by subsequent subscribers.
     * The original inventory hook reads cancellation, not this replacement value.
     * @param selectedSlot replacement requested-slot value
     */
    public void setSelectedSlot(int selectedSlot) {
        this.selectedSlot = selectedSlot;
    }
}
