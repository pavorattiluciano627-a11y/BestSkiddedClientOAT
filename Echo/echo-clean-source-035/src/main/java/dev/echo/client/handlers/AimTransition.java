package dev.echo.client.handlers;

/** How a requested rotation approaches its target. Declaration order is preserved. */
public enum AimTransition {
    SNAP,
    EASED,
    NATURAL
}
