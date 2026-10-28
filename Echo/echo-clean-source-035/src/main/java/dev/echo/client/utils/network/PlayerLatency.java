package dev.echo.client.utils.network;

import dev.echo.client.MinecraftAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

/** Queries the local player's server-reported latency. */
public final class PlayerLatency {
    public PlayerLatency() { }

    /**
     * Returns the local player's latency, or zero when its list entry is absent.
     *
     * @return latency in milliseconds
     */
    public static int milliseconds() {
        Minecraft client = MinecraftAccess.CLIENT;
        if (client.player == null || client.getConnection() == null) {
            return 0;
        }
        PlayerInfo entry = client.getConnection().getPlayerInfo(client.player.getUUID());
        return entry == null ? 0 : entry.getLatency();
    }

    /**
     * Converts latency to the nearest 50-millisecond server tick.
     *
     * @return the rounded latency in ticks
     */
    public static int ticks() {
        return Math.round(milliseconds() / 50.0F);
    }
}
