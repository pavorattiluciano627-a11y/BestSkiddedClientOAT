package dev.echo.client.utils;

import net.minecraft.client.multiplayer.ClientLevel;

/** Applies the reference client's fixed-time update to both world clocks. */
public final class ClientWorldUpdates {
    private ClientWorldUpdates() {
    }

    public static void setFixedTime(ClientLevel world, long time) {
        world.setTimeFromServer(time);
    }
}
