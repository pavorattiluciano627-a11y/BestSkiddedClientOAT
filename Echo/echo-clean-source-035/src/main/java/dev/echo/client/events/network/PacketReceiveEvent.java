package dev.echo.client.events.network;

import dev.echo.client.events.Event;
import net.minecraft.network.protocol.Packet;

/** Reports an incoming packet before handling; cancellation suppresses its delivery. */
public class PacketReceiveEvent extends Event {
    private Packet<?> packet;

    /**
     * Records the packet before the connection handles it.
     * @param packet incoming packet reference
     */
    public PacketReceiveEvent(Packet<?> packet) {
        this.packet = packet;
    }

    /** @return the incoming packet */
    public Packet<?> getPacket() {
        return packet;
    }
}
