package dev.echo.client.hud;

/** Mutable lifetime state shared between the original pending and visible queues. */
final class NotificationEntry {
    final NotificationMessage message;
    long visibleNanos;
    boolean exiting;
    boolean dismissWhenVisible;
    boolean forcedExit;

    NotificationEntry(NotificationMessage message) {
        this.message = message;
    }
}
