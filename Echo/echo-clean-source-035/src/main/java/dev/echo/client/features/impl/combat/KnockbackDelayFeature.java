package dev.echo.client.features.impl.combat;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.interaction.EntityAttackEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.network.PacketReceiveEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.utils.network.VelocityPackets;
import dev.echo.client.utils.strings.Concat;
import dev.echo.client.utils.targeting.TargetSelection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

/** Holds incoming packets behind a sampled knockback delay, then replays them in order. */
public class KnockbackDelayFeature extends Feature {
    private static final CharSequence rangeSeparator = CatalogText.literal("-");
    private static final CharSequence groundDelayOption = CatalogText.literal("ms");
    private static final CharSequence holdingSuffix = CatalogText.literal("Holding");
    private static final CharSequence chanceOption = CatalogText.literal("%");
    private static IntSetting chance;
    private static FloatRangeSetting airDelay;
    private static FloatRangeSetting groundDelay;
    private Queue<DelayedKnockbackPacket> delayedPackets;
    private boolean delayActive;
    private float delayMillis;
    private int trackedEntityId;
    private static String airDelayTooltip;
    private static String groundDelayTooltip;

    public KnockbackDelayFeature() {
        super(new dev.echo.client.features.FeatureInfo(CatalogText.literal("Knockback Delay"),
                CatalogText.literal("Delays incoming knockback packets"), dev.echo.client.features.Category.COMBAT));
    }

    @Override
    public void postInitialize() {
        delayedPackets = new ConcurrentLinkedDeque<>();
        trackedEntityId = -1;
        if (airDelayTooltip == null) airDelayTooltip = new String("Timing");
        airDelay.tooltip(airDelayTooltip);
        if (groundDelayTooltip == null) groundDelayTooltip = new String("Timing");
        groundDelay.tooltip(groundDelayTooltip);
    }

    @Override
    public CharSequence getSuffix() {
        if (delayActive) return holdingSuffix;
        boolean airborne = client.player != null && !client.player.isOnGround();
        FloatRangeSetting range = airborne ? airDelay : groundDelay;
        return Concat.sequences(Concat.integer((int) range.getLower()), rangeSeparator,
                Concat.integer((int) range.getUpper()), groundDelayOption);
    }

    @Override
    public void onEnable() {
        delayedPackets.clear();
        delayActive = false;
        delayMillis = 0.0f;
        trackedEntityId = -1;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        flushKnockbackPackets();
        resetDelayState();
        super.onDisable();
    }

    private void resetDelayState() {
        delayedPackets.clear();
        delayActive = false;
        delayMillis = 0.0f;
        trackedEntityId = -1;
    }

    @Subscribe
    private void onAttack(EntityAttackEvent.Pre event) {
        if (isOutsideWorld() || event.getTarget() == null) return;
        trackedEntityId = event.getTarget().getId();
    }

    @Subscribe
    private void onClientTick(ClientTickEvent event) {
        if (!delayActive) return;
        if (isTrackedTargetInvalid()) {
            flushKnockbackPackets();
            trackedEntityId = -1;
            return;
        }
        DelayedKnockbackPacket first = delayedPackets.peek();
        if (first == null) {
            delayActive = false;
            delayMillis = 0.0f;
            return;
        }
        if ((float) (System.currentTimeMillis() - first.queuedAtMillis) >= delayMillis) {
            flushKnockbackPackets();
        }
    }

    private boolean isTrackedTargetInvalid() {
        if (trackedEntityId == -1) return false;
        if (client.world == null) return true;
        Entity target = client.world.getEntityById(trackedEntityId);
        if (target == null || !target.isAlive() || target.isRemoved()) return true;
        if (target instanceof LivingEntity living) {
            if (living.getHealth() <= 0.0f) return true;
            return !TargetSelection.isValidTarget(living);
        }
        return false;
    }

    @Subscribe
    private void onPacketReceive(PacketReceiveEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.isConnectedToLocalServer(client)) return;
        Packet<?> packet = event.getPacket();
        if (packet == null) return;
        if (packet instanceof ChatMessageC2SPacket || packet instanceof GameMessageS2CPacket
                || packet instanceof CommandExecutionC2SPacket) return;
        if (packet instanceof PlayerPositionLookS2CPacket || packet instanceof DisconnectS2CPacket) {
            flushKnockbackPackets();
            return;
        }
        if (packet instanceof HealthUpdateS2CPacket health && health.getHealth() <= 0.0f) {
            flushKnockbackPackets();
            return;
        }
        boolean targetRemoved = false;
        if (packet instanceof EntitiesDestroyS2CPacket removed && trackedEntityId != -1
                && removed.getEntityIds().contains(trackedEntityId)) {
            trackedEntityId = -1;
            targetRemoved = true;
        }
        if (!delayActive && isPlayerKnockback(packet) && passesChance()) {
            delayActive = true;
            delayMillis = sampleDelayMillis();
        }
        if (!delayActive) return;
        delayedPackets.add(new DelayedKnockbackPacket(packet, System.currentTimeMillis()));
        event.cancel();
        if (targetRemoved) {
            flushKnockbackPackets();
            return;
        }
        DelayedKnockbackPacket first = delayedPackets.peek();
        if (first != null && (float) (System.currentTimeMillis() - first.queuedAtMillis) >= delayMillis) {
            flushKnockbackPackets();
        }
    }

    private boolean passesChance() {
        int percent = chance.getValue();
        if (percent >= 100) return true;
        if (percent <= 0) return false;
        return Math.random() * 100.0 < percent;
    }

    private float sampleDelayMillis() {
        boolean airborne = client.player != null && !client.player.isOnGround();
        return (airborne ? airDelay : groundDelay).sampleGaussian();
    }

    private boolean isPlayerKnockback(Packet<?> packet) {
        if (client.player == null) return false;
        if (packet instanceof EntityVelocityUpdateS2CPacket velocity) {
            return VelocityPackets.entityId(velocity) == client.player.getId();
        }
        return packet instanceof ExplosionS2CPacket explosion && explosion.playerKnockback().isPresent();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void flushKnockbackPackets() {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) {
            delayedPackets.clear();
            delayActive = false;
            delayMillis = 0.0f;
            return;
        }
        DelayedKnockbackPacket delayed;
        while ((delayed = delayedPackets.poll()) != null) {
            try {
                ((Packet) delayed.packet).apply(handler);
            } catch (Throwable ignored) {
                // The original continues replaying after any individual packet failure.
            }
        }
        delayActive = false;
        delayMillis = 0.0f;
    }

    public static void appendBootstrapSettings(KnockbackDelayFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, chance);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, airDelay);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, groundDelay);
    }

    public static CharSequence bootstrapGroundDelayOption() { return groundDelayOption; }

    public static CharSequence bootstrapChanceOption() { return chanceOption; }

    public static void assignBootstrapSettings(KnockbackDelayFeature feature, FloatRangeSetting preparedGroundDelay,
            FloatRangeSetting preparedAirDelay, IntSetting preparedChance) {
        groundDelay = preparedGroundDelay;
        airDelay = preparedAirDelay;
        chance = preparedChance;
    }
}
