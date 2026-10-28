package dev.echo.client.utils.network;

import dev.echo.client.MinecraftAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;

/** Packet sending and fixed-point relative-coordinate decoding. */
public final class Packets implements MinecraftAccess {
    private static final double RELATIVE_COORDINATE_SCALE = 4096.0;

    /** Preserves the original utility class's public constructor. */
    public Packets() { }

    /**
     * Sends a packet when the client has a network handler.
     *
     * @param packet the packet to send
     */
    public static void send(Packet<?> packet) {
        Minecraft client = CLIENT;
        if (client.getConnection() == null) {
            return;
        }
        client.getConnection().send(packet);
    }

    /**
     * Applies a protocol relative displacement to a coordinate.
     *
     * <p>A zero displacement returns the original double unchanged, including its
     * signed zero. Nonzero displacement rounds the base before adding the delta;
     * the addition intentionally retains Java long overflow semantics.
     *
     * @param coordinate the existing coordinate
     * @param delta the displacement in units of 1/4096 block
     * @return the resulting coordinate
     */
    public static double applyRelativeDelta(double coordinate, long delta) {
        return delta == 0L ? coordinate
                : (Math.round(coordinate * RELATIVE_COORDINATE_SCALE) + delta) / RELATIVE_COORDINATE_SCALE;
    }
}
