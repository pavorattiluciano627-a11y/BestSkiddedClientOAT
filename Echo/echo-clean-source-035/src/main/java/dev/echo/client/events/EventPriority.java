package dev.echo.client.events;

/** Original natural enum order is the event bus's subscriber execution order. */
public enum EventPriority {
    HIGHEST,
    HIGH,
    NORMAL,
    LOW,
    LOWEST
}
