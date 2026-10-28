package dev.echo.client.utils.network;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

/** Provides the entity and vector carried by an entity velocity packet. */
public final class VelocityPackets {
    private VelocityPackets() { }

    /**
     * Returns the packet's entity identifier.
     *
     * @param packet the velocity packet
     * @return the target entity identifier
     */
    public static int entityId(ClientboundSetEntityMotionPacket packet) {
        return packet.id();
    }

    /**
     * Returns the packet's decoded velocity vector.
     *
     * @param packet the velocity packet
     * @return the velocity vector
     */
    public static Vec3 velocity(ClientboundSetEntityMotionPacket packet) {
        return packet.movement();
    }
}
