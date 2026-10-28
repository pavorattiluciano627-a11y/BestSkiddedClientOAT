package dev.echo.client.bridge;

/** Renders queued GUI commands using the no-fog buffer. */
public interface GameRenderControl {
    void renderGuiWithoutFog();
}
