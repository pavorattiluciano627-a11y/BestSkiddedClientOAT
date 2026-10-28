package dev.echo.client.handlers;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.interaction.EntityAttackEvent;
import dev.echo.client.events.network.PacketReceiveEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.tag.DamageTypeTags;

/** Tracks attack age, damage confirmations and latency-adjusted hurt windows. */
public class AttackCooldownTracker extends ClientHandler {
    private static final int ATTACK_COOLDOWN_TICKS = 10;
    private static final Map<UUID, Long> attackTicks = new HashMap<>();
    private static final Map<Integer, Integer> damageTicks = new ConcurrentHashMap<>();

    /** Returns the local-player age recorded at the most recent attack on this entity. */
    public static long lastAttackTick(LivingEntity entity) {
        return entity == null ? 0L : attackTicks.getOrDefault(entity.getUuid(), 0L);
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
        DamageSource source = entity.getRecentDamageSource();
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
        if (confirmedAt == null || !attackTicks.containsKey(entity.getUuid())) {
            return 0;
        }
        int ticksSinceConfirmation = CLIENT.player.age - confirmedAt;
        return Math.max(0, ticksSinceAttack(entity) - ticksSinceConfirmation - latencyTicks());
    }

    public static boolean hasNonFireHurtWindow(LivingEntity entity) {
        if (entity == null || CLIENT.player == null || !allowsHurtTimePrediction()) {
            return false;
        }
        DamageSource source = entity.getRecentDamageSource();
        return source != null
                && !source.isIn(DamageTypeTags.IS_FIRE)
                && entity.hurtTime > latencyTicks();
    }

    private static boolean ignoresHurtTime(DamageSource source) {
        if (source.getAttacker() == CLIENT.player) {
            return false;
        }
        if (source.isIn(DamageTypeTags.IS_FIRE)) {
            return true;
        }
        return source.getAttacker() == null
                && !source.isIn(DamageTypeTags.IS_FALL)
                && !source.isIn(DamageTypeTags.IS_EXPLOSION);
    }

    private static int ticksSinceAttack(LivingEntity entity) {
        Long attackedAt = attackTicks.get(entity.getUuid());
        return attackedAt == null ? Integer.MAX_VALUE : (int) ((long) CLIENT.player.age - attackedAt);
    }

    private static int latencyTicks() {
        return Math.round(LatencyTracker.milliseconds() / 50.0F);
    }

    private static boolean allowsHurtTimePrediction() {
        if (CLIENT.getNetworkHandler() == null) {
            return false;
        }
        String brand = CLIENT.getNetworkHandler().getBrand();
        return ServerBrandPolicy.allowsHurtTimePrediction(brand);
    }

    @Subscribe
    private void onAttack(EntityAttackEvent.Post event) {
        if (CLIENT.player != null && event.getTarget() instanceof LivingEntity target) {
            attackTicks.put(target.getUuid(), (long) CLIENT.player.age);
        }
    }

    @Subscribe
    private void onPacketReceived(PacketReceiveEvent event) {
        if (event.getPacket() instanceof EntityDamageS2CPacket damage) {
            if (CLIENT.player != null && damage.sourceCauseId() == CLIENT.player.getId()) {
                damageTicks.put(damage.entityId(), CLIENT.player.age);
            }
            return;
        }
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket
                || event.getPacket() instanceof PlayerRespawnS2CPacket) {
            attackTicks.clear();
            damageTicks.clear();
        }
    }
}
