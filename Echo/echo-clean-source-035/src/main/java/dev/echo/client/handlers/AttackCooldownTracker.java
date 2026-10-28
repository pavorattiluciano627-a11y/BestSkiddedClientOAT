package dev.echo.client.handlers;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.interaction.EntityAttackEvent;
import dev.echo.client.events.network.PacketReceiveEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Tracks attack age, damage confirmations and latency-adjusted hurt windows. */
public class AttackCooldownTracker extends ClientHandler {
    private static final int ATTACK_COOLDOWN_TICKS = 10;
    private static final Map<UUID, Long> attackTicks = new HashMap<>();
    private static final Map<Integer, Integer> damageTicks = new ConcurrentHashMap<>();

    /** Returns the local-player age recorded at the most recent attack on this entity. */
    public static long lastAttackTick(LivingEntity entity) {
        return entity == null ? 0L : attackTicks.getOrDefault(entity.getUUID(), 0L);
    }

    public static boolean hasAttackCooldown(LivingEntity entity) {
        return remainingAttackCooldown(entity) > 0;
    }

    /** Combines the tracked attack interval with the target's applicable hurt animation. */
    public static int remainingAttackCooldown(LivingEntity entity) {
        if (entity == null || CLIENT.player == null) {
            return 0;
        }
        int remaining = ATTACK_COOLDOWN_TICKS - ticksSinceAttack(entity);
        DamageSource source = entity.getLastDamageSource();
        if (source != null && !ignoresHurtTime(source)) {
            remaining = Math.max(remaining, entity.hurtTime - latencyTicks());
        }
        return Math.max(remaining, 0);
    }

    /** Returns the nonnegative attack-to-confirmation delay after subtracting latency. */
    public static int confirmedDamageDelayTicks(LivingEntity entity) {
        if (entity == null || CLIENT.player == null) {
            return 0;
        }
        Integer confirmedAt = damageTicks.get(entity.getId());
        if (confirmedAt == null || !attackTicks.containsKey(entity.getUUID())) {
            return 0;
        }
        int ticksSinceConfirmation = CLIENT.player.tickCount - confirmedAt;
        return Math.max(0, ticksSinceAttack(entity) - ticksSinceConfirmation - latencyTicks());
    }

    public static boolean hasNonFireHurtWindow(LivingEntity entity) {
        if (entity == null || CLIENT.player == null || !allowsHurtTimePrediction()) {
            return false;
        }
        DamageSource source = entity.getLastDamageSource();
        return source != null
                && !source.is(DamageTypeTags.IS_FIRE)
                && entity.hurtTime > latencyTicks();
    }

    private static boolean ignoresHurtTime(DamageSource source) {
        if (source.getEntity() == CLIENT.player) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_FIRE)) {
            return true;
        }
        return source.getEntity() == null
                && !source.is(DamageTypeTags.IS_FALL)
                && !source.is(DamageTypeTags.IS_EXPLOSION);
    }

    private static int ticksSinceAttack(LivingEntity entity) {
        Long attackedAt = attackTicks.get(entity.getUUID());
        return attackedAt == null ? Integer.MAX_VALUE : (int) ((long) CLIENT.player.tickCount - attackedAt);
    }

    private static int latencyTicks() {
        return Math.round(LatencyTracker.milliseconds() / 50.0F);
    }

    private static boolean allowsHurtTimePrediction() {
        if (CLIENT.getConnection() == null) {
            return false;
        }
        String brand = CLIENT.getConnection().serverBrand();
        return ServerBrandPolicy.allowsHurtTimePrediction(brand);
    }

    @Subscribe
    private void onAttack(EntityAttackEvent.Post event) {
        if (CLIENT.player != null && event.getTarget() instanceof LivingEntity target) {
            attackTicks.put(target.getUUID(), (long) CLIENT.player.tickCount);
        }
    }

    @Subscribe
    private void onPacketReceived(PacketReceiveEvent event) {
        if (event.getPacket() instanceof ClientboundDamageEventPacket damage) {
            if (CLIENT.player != null && damage.sourceCauseId() == CLIENT.player.getId()) {
                damageTicks.put(damage.entityId(), CLIENT.player.tickCount);
            }
            return;
        }
        if (event.getPacket() instanceof ClientboundPlayerPositionPacket
                || event.getPacket() instanceof ClientboundRespawnPacket) {
            attackTicks.clear();
            damageTicks.clear();
        }
    }
}
