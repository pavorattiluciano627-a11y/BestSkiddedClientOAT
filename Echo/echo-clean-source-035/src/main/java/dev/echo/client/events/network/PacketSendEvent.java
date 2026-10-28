package dev.echo.client.events.network;

import dev.echo.client.events.Event;
import net.minecraft.network.packet.Packet;

/** Intercepts an outgoing packet and allows its immediate flush to be suppressed. */
public class PacketSendEvent extends Event {
    private final Packet<?> packet;
    private boolean flush;

    /**
     * Records an outgoing packet and initially permits flushing.
     * @param packet outgoing packet reference
     */
    public PacketSendEvent(Packet<?> packet) {
        this.flush = true;
        this.packet = packet;
    }

    /** @return the outgoing packet */
    public Packet<?> getPacket() {
        return packet;
    }

    /** @return whether this event permits the caller's requested flush */
    public boolean shouldFlush() {
        return flush;
    }

    /**
     * Controls flushing independently of cancellation.
     * Enabling this flag does not force a flush if the caller requested none.
     * @param flush whether to permit an immediate flush
     */
    public void setFlush(boolean flush) {
        this.flush = flush;
    }
}
