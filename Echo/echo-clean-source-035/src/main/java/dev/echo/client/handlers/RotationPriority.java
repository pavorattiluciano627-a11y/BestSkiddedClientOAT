package dev.echo.client.handlers;

/** Request priorities in descending order, matching the original enum order. */
public enum RotationPriority {
    CRITICAL, HIGH, NORMAL, LOW;

    /** Returns whether this priority strictly outranks the other request. */
    public boolean higherThan(RotationPriority other) {
        return ordinal() < other.ordinal();
    }
}
