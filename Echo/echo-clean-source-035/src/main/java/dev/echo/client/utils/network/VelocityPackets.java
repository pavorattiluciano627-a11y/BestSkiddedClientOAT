package dev.echo.client.utils.network;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;

/** Provides the entity and vector carried by an entity velocity packet. */
public final class VelocityPackets {
    private VelocityPackets() { }

    /**
     * Returns the packet's entity identifier.
     *
     * @param packet the velocity packet
     * @return the target entity identifier
     */
    public static int entityId(EntityVelocityUpdateS2CPacket packet) {
        return packet.getEntityId();
    }

    /**
     * Returns the packet's decoded velocity vector.
     *
     * @param packet the velocity packet
     * @return the velocity vector
     */
    public static Vec3d velocity(EntityVelocityUpdateS2CPacket packet) {
        return packet.getVelocity();
    }
}
