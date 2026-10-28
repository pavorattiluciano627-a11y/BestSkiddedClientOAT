package dev.echo.client.handlers;

import dev.echo.client.events.EventPriority;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.interaction.EntityAttackEvent;
import dev.echo.client.events.network.PacketSendEvent;
import dev.echo.client.utils.targeting.EntityPredicates;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/** Tracks the fall distance communicated by uncancelled movement packets. */
public final class ServerFallDistanceTracker extends ClientHandler {
    private static final double MACE_SMASH_THRESHOLD = 1.5;
    private static LocalPlayer trackedPlayer;
    private static double fallDistance;
    private static double lastSentY;
    private static boolean hasSentPosition;

    public static double fallDistance(Player player) {
        if (player == null || player != trackedPlayer) {
            return 0.0;
        }
        return Math.min(fallDistance, player.fallDistance);
    }

    @Subscribe(priority = EventPriority.LOWEST)
    private void onPacketSent(PacketSendEvent event) {
        if (event.cancelled || CLIENT.player == null) {
            return;
        }
        if (CLIENT.player != trackedPlayer) {
            trackedPlayer = CLIENT.player;
            fallDistance = 0.0;
            hasSentPosition = false;
        }
        if (event.getPacket() instanceof ServerboundAcceptTeleportationPacket) {
            fallDistance = 0.0;
            hasSentPosition = false;
            return;
        }
        if (!(event.getPacket() instanceof ServerboundMovePlayerPacket movement)) {
            return;
        }
        double y = movement.getY(lastSentY);
        double deltaY = hasSentPosition ? y - lastSentY : 0.0;
        if (movement.hasPosition()) {
            lastSentY = y;
            hasSentPosition = true;
        }
        if (deltaY < 0.0 && !CLIENT.player.isInWater()) {
            fallDistance -= deltaY;
        }
        if (movement.isOnGround() || deltaY > 0.0) {
            fallDistance = 0.0;
        }
    }

    @Subscribe
    private void onAttack(EntityAttackEvent.Post event) {
        Player attacker = event.getPlayer();
        if (attacker == null || attacker != trackedPlayer || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        if (!attacker.getMainHandItem().is(Items.MACE)
                || attacker.isFallFlying()
                || fallDistance(attacker) <= MACE_SMASH_THRESHOLD) {
            return;
        }
        if (target instanceof Player player
                && EntityPredicates.isBlocking(player)
                && !EntityPredicates.isOutsideBlockingArc(player)) {
            return;
        }
        fallDistance = 0.0;
    }
}
