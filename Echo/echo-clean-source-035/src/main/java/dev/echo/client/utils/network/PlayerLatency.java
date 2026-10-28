package dev.echo.client.utils.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import dev.echo.client.MinecraftAccess;

/** Queries the local player's server-reported latency. */
public final class PlayerLatency {
    public PlayerLatency() { }

    /**
     * Returns the local player's latency, or zero when its list entry is absent.
     *
     * @return latency in milliseconds
     */
    public static int milliseconds() {
        MinecraftClient client = MinecraftAccess.CLIENT;
        if (client.player == null || client.getNetworkHandler() == null) {
            return 0;
        }
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
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
