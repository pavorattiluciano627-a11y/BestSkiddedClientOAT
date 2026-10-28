package dev.echo.client.feature.internals;

/** A deferred cleanup operation collected during the client object-graph walk. */
@FunctionalInterface
public interface ClientCleanupAction {
    /** Performs the collected operation. */
    void clear();
}
