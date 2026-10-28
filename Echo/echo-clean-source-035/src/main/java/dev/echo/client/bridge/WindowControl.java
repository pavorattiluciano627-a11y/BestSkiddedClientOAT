package dev.echo.client.bridge;

/** Implemented by the window mixin to invalidate the applied fullscreen state. */
public interface WindowControl {
    /** Clears the applied-state marker when fullscreen is requested. */
    void invalidateFullscreenState();
}
