package dev.echo.client.features.impl.combat;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.EventPriority;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.interaction.EntityAttackEvent;
import dev.echo.client.events.lifecycle.RenderTickEvent;
import dev.echo.client.events.lifecycle.WorldTickContextEvent;
import dev.echo.client.events.network.PacketReceiveEvent;
import dev.echo.client.events.render.WorldRenderEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.handlers.AttackCooldownTracker;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.render.OverlayWorldGeometry;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.HotbarSlotsSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.setting.ItemListSetting;
import dev.echo.client.utils.ActionTimer;
import dev.echo.client.utils.RandomMath;
import dev.echo.client.utils.network.Packets;
import dev.echo.client.utils.network.VelocityPackets;
import dev.echo.client.utils.strings.Concat;
import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Buffers incoming packets while a target's server position is farther away than its client position. */
public class BacktrackFeature extends Feature {
    private static final CharSequence rangeSeparator = CatalogText.literal("-");
    private static final CharSequence millisecondsSuffix = CatalogText.literal("ms");
    private static FloatRangeSetting holdTime;
    private static IntSetting maximumHurtTime;
    private static ItemListSetting itemWhitelist;
    private static HotbarSlotsSetting hotbarWhitelist;
    private static ColorSetting fillColor;
    private static ColorSetting outlineColor;
    private ArrayDeque<DelayedPacket> delayedPackets;
    private ActionTimer trackingTimer;
    private LivingEntity trackedTarget;
    private long holdMillis;
    private Vec3d serverTargetPosition;
    private Vec3d previousRenderPosition;
    private Vec3d renderPosition;
    private static String timingTooltip;
    private static String itemTooltip;
    private static String hotbarTooltip;
    private static String fillTooltip;
    private static String outlineTooltip;

    public BacktrackFeature() {
        super(new FeatureInfo(CatalogText.literal("Backtrack"),
                CatalogText.literal("Attacks targets at their previous positions"), Category.COMBAT));
    }

    @Override
    public void postInitialize() {
        delayedPackets = new ArrayDeque<>();
        trackingTimer = new ActionTimer();
        holdTime.setAllowOutOfRange(true);
        maximumHurtTime.setAllowOutOfRange(true);
        maximumHurtTime.description(CatalogText.literal("Activates when the target has at most this many hurt ticks remaining."));
        itemWhitelist.description(CatalogText.literal("Activates only with selected items. Empty allows every item."));
        if (timingTooltip == null) timingTooltip = new String("Timing");
        holdTime.tooltip(timingTooltip);
        if (itemTooltip == null) itemTooltip = new String("Conditions");
        itemWhitelist.tooltip(itemTooltip);
        if (hotbarTooltip == null) hotbarTooltip = new String("Conditions");
        hotbarWhitelist.tooltip(hotbarTooltip);
        if (fillTooltip == null) fillTooltip = new String("Appearance");
        fillColor.tooltip(fillTooltip);
        if (outlineTooltip == null) outlineTooltip = new String("Appearance");
        outlineColor.tooltip(outlineTooltip);
    }

    @Override
    public CharSequence getSuffix() {
        int lower = (int) holdTime.getLower();
        int upper = (int) holdTime.getUpper();
        return lower == upper ? Concat.sequences(Concat.integer(lower), millisecondsSuffix)
                : Concat.sequences(Concat.integer(lower), rangeSeparator, Concat.integer(upper), millisecondsSuffix);
    }

    @Override
    public synchronized void onEnable() {
        resetTracking(false);
        super.onEnable();
    }

    @Override
    public synchronized void onDisable() {
        resetTracking(true);
        super.onDisable();
    }

    @Subscribe
    private synchronized void onWorldContext(WorldTickContextEvent event) {
        resetTracking(false);
    }

    @Subscribe
    private synchronized void onAttack(EntityAttackEvent.Pre event) {
        if (isOutsideWorld() || MinecraftRenderState.isConnectedToLocalServer(client)) return;
        if (!(event.getTarget() instanceof LivingEntity target) || !target.isAlive()) return;
        if (trackedTarget != null) {
            if (trackedTarget == target) return;
            clearTrackedTarget();
        }
        if (!itemWhitelist.acceptsHeldItem() || !hotbarWhitelist.permitsCurrentSlot()) return;
        if (AttackCooldownTracker.remainingAttackCooldown(target) > maximumHurtTime.getValue()) return;
        trackedTarget = target;
        trackingTimer.reset();
        holdMillis = Math.max(0L, Math.round(RandomMath.randomDouble(holdTime.getLower(), holdTime.getUpper())));
        Vec3d position = target.getTrackedPosition().getPos();
        renderPosition = position;
        previousRenderPosition = position;
        serverTargetPosition = position;
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    private synchronized void onPacketReceive(PacketReceiveEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.isConnectedToLocalServer(client)) return;
        Packet<?> packet = event.getPacket();
        if (packet == null) return;
        // Inspect the entire bundle before changing positions or cancelling its envelope.
        for (Packet<?> child : unpackBundle(packet)) {
            if (endsTracking(child) || (trackedTarget != null
                    && (!isTrackedTargetValid() || removesTrackedTarget(child)))) {
                clearTrackedTarget();
                return;
            }
            if (requiresImmediateReplay(child)) {
                replayThrough(Long.MAX_VALUE);
                return;
            }
        }
        if (trackedTarget == null) return;
        for (Packet<?> child : unpackBundle(packet)) updateServerPosition(child);
        if (isServerPositionFarther()) {
            delayedPackets.add(new DelayedPacket(packet, trackingTimer.currentMillis()));
            event.cancel();
        } else if (!delayedPackets.isEmpty()) {
            clearTrackedTarget();
        }
    }

    private static Iterable<? extends Packet<?>> unpackBundle(Packet<?> packet) {
        return packet instanceof BundleS2CPacket bundle ? bundle.getPackets() : List.of(packet);
    }

    private boolean removesTrackedTarget(Packet<?> packet) {
        return packet instanceof EntitiesDestroyS2CPacket removed
                && removed.getEntityIds().contains(trackedTarget.getId());
    }

    @Subscribe
    private synchronized void onRenderTick(RenderTickEvent event) {
        if (trackedTarget == null) return;
        if (!isTrackedTargetValid()) {
            clearTrackedTarget();
            return;
        }
        updateRenderPosition();
        if (!delayedPackets.isEmpty()) replayThrough(trackingTimer.currentMillis() - holdMillis);
    }

    @Subscribe
    private synchronized void onWorldRender(WorldRenderEvent event) {
        boolean fill = fillColor.getAlpha() > 0;
        boolean outline = outlineColor.getAlpha() > 0;
        if (trackedTarget == null || delayedPackets.isEmpty() || (!fill && !outline)) return;
        Vec3d position = previousRenderPosition.lerp(renderPosition, (double) event.getTickProgress());
        Box box = trackedTarget.getBoundingBox().offset(position.subtract(trackedTarget.getEntityPos()));
        OverlayWorldGeometry geometry = event.getRenderContext().getOverlayGeometry();
        if (fill) geometry.drawBox(box.minX, box.minY, box.minZ,
                box.getLengthX(), box.getLengthY(), box.getLengthZ(), fillColor.getArgb());
        if (outline) drawTargetOutline(geometry, box, outlineColor.getArgb());
    }

    private boolean isTrackedTargetValid() {
        return trackedTarget != null && trackedTarget.isAlive() && !trackedTarget.isRemoved()
                && itemWhitelist.acceptsHeldItem() && hotbarWhitelist.permitsCurrentSlot();
    }

    private boolean endsTracking(Packet<?> packet) {
        return packet instanceof DisconnectS2CPacket || packet instanceof PlayerRespawnS2CPacket
                || (packet instanceof HealthUpdateS2CPacket health && health.getHealth() <= 0.0f);
    }

    private boolean requiresImmediateReplay(Packet<?> packet) {
        if (packet instanceof PlayerPositionLookS2CPacket) return true;
        if (client.player == null) return false;
        if (packet instanceof EntityDamageS2CPacket damage && damage.entityId() == client.player.getId()) return true;
        return packet instanceof EntityVelocityUpdateS2CPacket velocity
                && VelocityPackets.entityId(velocity) == client.player.getId();
    }

    private void updateServerPosition(Packet<?> packet) {
        if (packet instanceof EntityS2CPacket relative && relative.isPositionChanged() && client.world != null) {
            Entity entity = relative.getEntity(client.world);
            if (entity != null && entity.getId() == trackedTarget.getId()) {
                serverTargetPosition = new Vec3d(
                        Packets.applyRelativeDelta(serverTargetPosition.x, relative.getDeltaX()),
                        Packets.applyRelativeDelta(serverTargetPosition.y, relative.getDeltaY()),
                        Packets.applyRelativeDelta(serverTargetPosition.z, relative.getDeltaZ()));
            }
        } else if (packet instanceof EntityPositionSyncS2CPacket position && position.id() == trackedTarget.getId()) {
            serverTargetPosition = position.values().position();
        } else if (packet instanceof EntityPositionS2CPacket teleport && teleport.entityId() == trackedTarget.getId()) {
            Vec3d position = teleport.change().position();
            serverTargetPosition = new Vec3d(
                    teleport.relatives().contains(PositionFlag.X) ? serverTargetPosition.x + position.x : position.x,
                    teleport.relatives().contains(PositionFlag.Y) ? serverTargetPosition.y + position.y : position.y,
                    teleport.relatives().contains(PositionFlag.Z) ? serverTargetPosition.z + position.z : position.z);
        }
    }

    private boolean isServerPositionFarther() {
        if (client.player == null) return false;
        Vec3d eye = client.player.getEyePos();
        Vec3d current = trackedTarget.getTrackedPosition().getPos();
        return squaredDistanceToTargetAt(eye, serverTargetPosition) > squaredDistanceToTargetAt(eye, current);
    }

    private double squaredDistanceToTargetAt(Vec3d observer, Vec3d targetPosition) {
        return trackedTarget.getBoundingBox().offset(targetPosition.subtract(trackedTarget.getEntityPos()))
                .squaredMagnitude(observer);
    }

    private void updateRenderPosition() {
        if (renderPosition.squaredDistanceTo(serverTargetPosition) > 4096.0) renderPosition = serverTargetPosition;
        previousRenderPosition = renderPosition;
        renderPosition = renderPosition.lerp(serverTargetPosition, 0.5);
    }

    private void replayThrough(long cutoffMillis) {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        while (true) {
            DelayedPacket delayed = delayedPackets.peek();
            if (delayed == null || delayed.queuedAtMillis() > cutoffMillis) return;
            delayedPackets.poll();
            if (handler != null) {
                Packet<?> packet = delayed.packet();
                initializeResolvedClass(ClientPlayNetworkHandler.class);
                applyIgnoringFailure(handler, packet);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void applyIgnoringFailure(ClientPlayNetworkHandler handler, Packet<?> packet) {
        try {
            ((Packet) packet).apply(handler);
        } catch (Throwable ignored) {
            // Each packet failure is discarded; the remaining queue is still replayed.
        }
    }

    private synchronized void clearTrackedTarget() {
        replayThrough(Long.MAX_VALUE);
        initializeResolvedClass(LivingEntity.class);
        trackedTarget = null;
        initializeResolvedClass(Vec3d.class);
        renderPosition = null;
        initializeResolvedClass(Vec3d.class);
        previousRenderPosition = null;
        initializeResolvedClass(Vec3d.class);
        serverTargetPosition = null;
    }

    private static void initializeResolvedClass(Class<?> type) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }

    private synchronized void resetTracking(boolean flushPending) {
        if (!flushPending) delayedPackets.clear();
        clearTrackedTarget();
    }

    /** The original outline helper emits these twelve edges in this order. */
    private static void drawTargetOutline(OverlayWorldGeometry geometry, Box box, int color) {
        double x = box.minX, y = box.minY, z = box.minZ;
        double maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;
        geometry.drawLine(x, y, z, maxX, y, z, color);
        geometry.drawLine(maxX, y, z, maxX, y, maxZ, color);
        geometry.drawLine(maxX, y, maxZ, x, y, maxZ, color);
        geometry.drawLine(x, y, maxZ, x, y, z, color);
        geometry.drawLine(x, maxY, z, maxX, maxY, z, color);
        geometry.drawLine(maxX, maxY, z, maxX, maxY, maxZ, color);
        geometry.drawLine(maxX, maxY, maxZ, x, maxY, maxZ, color);
        geometry.drawLine(x, maxY, maxZ, x, maxY, z, color);
        geometry.drawLine(x, y, z, x, maxY, z, color);
        geometry.drawLine(maxX, y, z, maxX, maxY, z, color);
        geometry.drawLine(maxX, y, maxZ, maxX, maxY, maxZ, color);
        geometry.drawLine(x, y, maxZ, x, maxY, maxZ, color);
    }

    /** Value semantics belong to the Backtrack queue, unlike the knockback packet holder. */
    private record DelayedPacket(Packet<?> packet, long queuedAtMillis) {}

    public static void appendBootstrapSettings(BacktrackFeature feature) {
        CatalogSettings.append(feature, holdTime);
        CatalogSettings.append(feature, maximumHurtTime);
        CatalogSettings.append(feature, itemWhitelist);
        CatalogSettings.append(feature, hotbarWhitelist);
        CatalogSettings.append(feature, fillColor);
        CatalogSettings.append(feature, outlineColor);
    }

    public static void assignBootstrapSettings(BacktrackFeature feature, HotbarSlotsSetting preparedHotbarWhitelist,
            ColorSetting preparedOutlineColor, IntSetting preparedMaximumHurtTime, FloatRangeSetting preparedHoldTime,
            ItemListSetting preparedItemWhitelist, ColorSetting preparedFillColor) {
        hotbarWhitelist = preparedHotbarWhitelist;
        outlineColor = preparedOutlineColor;
        maximumHurtTime = preparedMaximumHurtTime;
        holdTime = preparedHoldTime;
        itemWhitelist = preparedItemWhitelist;
        fillColor = preparedFillColor;
    }
}
