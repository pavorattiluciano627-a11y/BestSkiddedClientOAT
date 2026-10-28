package dev.echo.client.features.impl.combat;

import net.minecraft.network.protocol.Packet;

/** A queued packet and its original enqueue time; identity equality is intentional. */
final class DelayedKnockbackPacket {
    final Packet<?> packet;
    final long queuedAtMillis;

    DelayedKnockbackPacket(Packet<?> packet, long queuedAtMillis) {
        this.packet = packet;
        this.queuedAtMillis = queuedAtMillis;
    }
}
