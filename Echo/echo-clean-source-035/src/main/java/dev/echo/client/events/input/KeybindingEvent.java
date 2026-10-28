package dev.echo.client.events.input;

import dev.echo.client.events.Event;

/**
 * Mutable click count retaining the original shared sentinel in this analysis type.
 * Production removal of that control state still requires the global proof.
 */
public abstract class KeybindingEvent extends Event {
    private final int initialClickCount;
    private int clickCount;
    private static String[] inputSentinel;

    static {
        if (getInputSentinel() != null) {
            setInputSentinel(new String[2]);
        }
    }

    protected KeybindingEvent(int clickCount) {
        super();
        this.initialClickCount = clickCount;
        this.clickCount = clickCount;
    }

    public int getClickCount() {
        return clickCount;
    }

    public int getClickCountDelta() {
        return clickCount - initialClickCount;
    }

    public void setClickCount(int clickCount) {
        this.clickCount = Math.max(0, clickCount);
    }

    public void addClick() {
        clickCount++;
    }

    /** Preserves both original sentinel branches, including JVM boolean narrowing. */
    public boolean hasClicks() {
        String[] sentinel = getInputSentinel();
        int count = clickCount;
        return sentinel != null ? (count & 1) != 0 : count > 0;
    }

    /** Returns the original shared reference, without copying or replacing it. */
    public static String[] getInputSentinel() {
        return inputSentinel;
    }

    /** Original shared-state write, retained only in this analysis candidate. */
    public static void setInputSentinel(String[] sentinel) {
        inputSentinel = sentinel;
    }
}
