package dev.echo.client;

import dev.echo.client.bridge.TickSpeedControl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

/** Common Minecraft client access used by client handlers and utility types. */
public interface MinecraftAccess {
    MinecraftClient CLIENT = MinecraftClient.getInstance();

    /** Updates the multiplier exposed by the dynamic render-tick-counter mixin. */
    static void setTickSpeed(float multiplier) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        RenderTickCounter counter = client.getRenderTickCounter();
        if (counter instanceof TickSpeedControl control) {
            control.setTickSpeedMultiplier(multiplier);
        }
    }
}
