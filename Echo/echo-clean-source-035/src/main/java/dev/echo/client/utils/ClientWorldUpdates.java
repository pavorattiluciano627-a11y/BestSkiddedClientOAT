package dev.echo.client.utils;

import net.minecraft.client.world.ClientWorld;

/** Applies the reference client's fixed-time update to both world clocks. */
public final class ClientWorldUpdates {
    private ClientWorldUpdates() {
    }

    public static void setFixedTime(ClientWorld world, long time) {
        world.setTime(time, time, false);
    }
}
