package dev.echo.client.bridge;

/** Render-tick multiplier implemented on Minecraft's dynamic tick counter. */
public interface TickSpeedControl {
    void setTickSpeedMultiplier(float multiplier);

    float getTickSpeedMultiplier();
}
