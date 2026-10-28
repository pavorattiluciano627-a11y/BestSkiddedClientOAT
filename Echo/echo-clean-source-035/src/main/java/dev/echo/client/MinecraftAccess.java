package dev.echo.client;

import dev.echo.client.bridge.TickSpeedControl;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;

/** Common Minecraft client access used by client handlers and utility types. */
public interface MinecraftAccess {
    Minecraft CLIENT = Minecraft.getInstance();

    /** Updates the multiplier exposed by the dynamic render-tick-counter mixin. */
    static void setTickSpeed(float multiplier) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        DeltaTracker counter = client.getDeltaTracker();
        if (counter instanceof TickSpeedControl control) {
            control.setTickSpeedMultiplier(multiplier);
        }
    }
}
