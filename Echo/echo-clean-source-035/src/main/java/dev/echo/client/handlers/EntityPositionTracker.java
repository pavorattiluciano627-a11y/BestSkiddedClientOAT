package dev.echo.client.handlers;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.network.PacketReceiveEvent;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;

/** Tracks bounded remote-player position history and received velocity snapshots. */
public final class EntityPositionTracker extends ClientHandler {
    private static final int MAX_HISTORY_SAMPLES = 8;
    private static final int UNCHANGED_POSITION_SAMPLE_TICKS = 3;
    private static final Map<Integer, ArrayDeque<PositionSample>> positionHistory = new HashMap<>();
    private static final Map<Integer, VelocitySample> velocitySamples = new HashMap<>();
    private static final ConcurrentLinkedQueue<EntityVelocityUpdateS2CPacket> receivedVelocityPackets =
            new ConcurrentLinkedQueue<>();
    private static Object trackedWorld;
    private static int trackerTick;

    /** Returns the tracker clock, which advances only while a world and player exist. */
    public static int currentTick() {
        return trackerTick;
    }

    /**
     * Copies an entity's recorded positions in oldest-to-newest order.
     * @param entity entity whose ID selects the history
     * @return an immutable snapshot, or an empty list when no history exists
     */
    public static List<PositionSample> positions(Entity entity) {
        ArrayDeque<PositionSample> history = positionHistory.get(entity.getId());
        return history == null ? List.of() : List.copyOf(history);
    }

    /**
     * Returns the most recent received velocity snapshot for an entity.
     * @param entity entity whose ID selects the snapshot
     * @return the snapshot, or null when no velocity has been recorded
     */
    public static VelocitySample velocity(Entity entity) {
        return velocitySamples.get(entity.getId());
    }

    @Subscribe
    private void onPacketReceived(PacketReceiveEvent event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof BundleS2CPacket bundle) {
            for (Packet<?> bundled : bundle.getPackets()) {
                if (bundled instanceof EntityVelocityUpdateS2CPacket velocity) {
                    receivedVelocityPackets.add(velocity);
                }
            }
        } else if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket velocity) {
            receivedVelocityPackets.add(velocity);
        }
    }

    @Subscribe
    private void onTick(ClientTickEvent event) {
        if (CLIENT.world == null || CLIENT.player == null) {
            positionHistory.clear();
            velocitySamples.clear();
            receivedVelocityPackets.clear();
            trackedWorld = null;
            return;
        }
        if (trackedWorld != CLIENT.world) {
            positionHistory.clear();
            velocitySamples.clear();
            trackedWorld = CLIENT.world;
        }
        trackerTick++;
        EntityVelocityUpdateS2CPacket packet;
        while ((packet = receivedVelocityPackets.poll()) != null) {
            Entity entity = CLIENT.world.getEntityById(packet.getEntityId());
            if (entity instanceof PlayerEntity player && player != CLIENT.player) {
                velocitySamples.put(player.getId(), new VelocitySample(
                        trackerTick, player.getTrackedPosition().getPos(), packet.getVelocity()));
            }
        }

        Set<Integer> presentPlayers = new HashSet<>();
        for (PlayerEntity player : CLIENT.world.getPlayers()) {
            if (player == CLIENT.player) {
                continue;
            }
            presentPlayers.add(player.getId());
            Vec3d position = player.getTrackedPosition().getPos();
            ArrayDeque<PositionSample> history = positionHistory.computeIfAbsent(
                    player.getId(), ignored -> new ArrayDeque<>());
            PositionSample previous = history.peekLast();
            if (previous != null && previous.position().equals(position)
                    && trackerTick - previous.tick() < UNCHANGED_POSITION_SAMPLE_TICKS) {
                continue;
            }
            history.addLast(new PositionSample(trackerTick, position, player.isOnGround()));
            if (history.size() > MAX_HISTORY_SAMPLES) {
                history.removeFirst();
            }
        }
        positionHistory.keySet().retainAll(presentPlayers);
        velocitySamples.keySet().retainAll(presentPlayers);
    }
}
