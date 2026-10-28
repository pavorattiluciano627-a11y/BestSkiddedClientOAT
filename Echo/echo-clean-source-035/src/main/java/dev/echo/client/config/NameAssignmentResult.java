package dev.echo.client.config;

/** Result of assigning a keyboard or mouse binding to a feature. */
public enum NameAssignmentResult {
    /** A new binding was assigned. */
    ASSIGNED,
    /** The binding was cleared. */
    CLEARED,
    /** The requested binding was already assigned. */
    UNCHANGED,
    /** The binding is reserved. */
    RESERVED
}
