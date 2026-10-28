package dev.echo.client.handlers;

/** Temporary rotation override; closing invalidates the guard before restoration. */
public final class RotationScope implements AutoCloseable {
    private final RotationManager manager;
    private boolean active = true;

    RotationScope(RotationManager manager) {
        this.manager = manager;
    }

    /** Marks the override used, so a completed movement packet can retain its visual pose. */
    public void commit() {
        if (active) manager.markScopeUsed(this);
    }

    /** Restores the previous rotation state at most once. */
    @Override
    public void close() {
        if (!active) return;
        active = false;
        manager.closeScope(this);
    }

    boolean isActive() {
        return active;
    }

    void invalidate() {
        active = false;
    }
}
