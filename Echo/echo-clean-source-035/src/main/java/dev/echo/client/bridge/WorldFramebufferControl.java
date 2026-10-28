package dev.echo.client.bridge;

/** Publishes the outline framebuffer event and remembers whether its next draw is cancelled. */
public interface WorldFramebufferControl {
    void updateOutlineFramebufferCancellation();
}
