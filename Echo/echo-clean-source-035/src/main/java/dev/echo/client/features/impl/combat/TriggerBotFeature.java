package dev.echo.client.features.impl.combat;

import dev.echo.client.Echo;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.AttackKeyEvent;
import dev.echo.client.events.network.PacketReceiveEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.handlers.AttackCooldownTracker;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.HotbarSlotsSetting;
import dev.echo.client.setting.ItemListSetting;
import dev.echo.client.utils.ActionTimer;
import dev.echo.client.utils.strings.Concat;
import dev.echo.client.utils.targeting.EntityPredicates;
import dev.echo.client.utils.targeting.EntityRaycasts;
import dev.echo.client.utils.targeting.TargetSelection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

/** Adds attack clicks after target acquisition, cooldown, reaction and critical-hit checks. */
public class TriggerBotFeature extends Feature {
    private static final CharSequence CRIT_NONE = CatalogText.literal("None");
    private static final CharSequence CRIT_PRECISE = CatalogText.literal("pCrit");
    private static final CharSequence CRIT_NORMAL = CatalogText.literal("Normal");
    private static final CharSequence EMPTY_SUFFIX = Concat.strings();
    private static ChoiceSetting critMode;
    private static BooleanSetting critOnlyOnSpace;
    private static BooleanSetting attackShields;
    private static BooleanSetting hurtTimeCheck;
    private static BooleanSetting noSweepOrPickHits;
    private static FloatRangeSetting cooldown;
    private static FloatRangeSetting critCooldown;
    private static FloatRangeSetting critRange;
    private static FloatSetting attackDistance;
    private static FloatRangeSetting reactionDelay;
    private static FloatRangeSetting attackDelay;
    private static ItemListSetting itemWhitelist;
    private static HotbarSlotsSetting hotbarWhitelist;
    private ActionTimer reactionTimer;
    private ActionTimer attackTimer;
    private ActionTimer criticalWindowTimer;
    private boolean targetAcquired;
    private long lastAttackTick;
    private long reactionDelayMillis;
    private long attackDelayMillis;
    private double sampledAttackCooldown;
    private static String cooldownTooltip;
    private static String timingTooltip;
    private static String itemWhitelistTooltip;
    private static String otherConditionsTooltip;

    public TriggerBotFeature() {
        super(new dev.echo.client.features.FeatureInfo(CatalogText.literal("Trigger Bot"),
                CatalogText.literal("Attacks when aiming at targets"), dev.echo.client.features.Category.COMBAT));
    }

    @Override
    public void postInitialize() {
        reactionTimer = new ActionTimer();
        attackTimer = new ActionTimer();
        criticalWindowTimer = new ActionTimer();
        lastAttackTick = -1L;
        sampledAttackCooldown = 0.9;
        critRange.setAllowOutOfRange(true);
        attackDistance.setAllowOutOfRange(true);
        attackDistance.displayAs(0.0f, CatalogText.literal("Vanilla Reach"));
        attackDelay.description(CatalogText.literal("Delay between attacks"));
        reactionDelay.description(CatalogText.literal("Time on target before attacking"));
        attackDistance.description(CatalogText.literal("Only attack when the crosshair hit on the hitbox is within this distance, 0 uses vanilla reach"));
        if (cooldownTooltip == null) cooldownTooltip = new String("Timing");
        cooldown.tooltip(cooldownTooltip);
        if (timingTooltip == null) timingTooltip = new String("Timing");
        critCooldown.tooltip(timingTooltip);
        reactionDelay.tooltip(timingTooltip);
        attackDelay.tooltip(timingTooltip);
        if (itemWhitelistTooltip == null) itemWhitelistTooltip = new String("Conditions");
        itemWhitelist.tooltip(itemWhitelistTooltip);
        if (otherConditionsTooltip == null) otherConditionsTooltip = new String("Conditions");
        hotbarWhitelist.tooltip(otherConditionsTooltip);
        noSweepOrPickHits.tooltip(otherConditionsTooltip);
    }

    @Override
    public CharSequence getSuffix() {
        return critMode.isSelected(CRIT_NONE) ? EMPTY_SUFFIX : critMode.getValue();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        reactionTimer.reset();
        attackTimer.reset();
        criticalWindowTimer.reset();
        targetAcquired = false;
        lastAttackTick = -1L;
        reactionDelayMillis = 0L;
        attackDelayMillis = 0L;
        sampleAttackCooldown();
    }

    @Subscribe
    private void onAttackKey(AttackKeyEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) return;
        if (!itemWhitelist.acceptsHeldItem() || !hotbarWhitelist.permitsCurrentSlot()) {
            resetTargetAcquisition();
            return;
        }
        float distance = attackDistance.getValue();
        EntityHitResult hit = findAttackTarget(distance);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            resetTargetAcquisition();
            return;
        }
        double squaredDistance = client.player.getEyePosition().distanceToSqr(hit.getLocation());
        if (distance > 0.0f && squaredDistance > (double) (distance * distance)) {
            resetTargetAcquisition();
            return;
        }
        if (!TargetSelection.isValidTarget(target)) {
            resetTargetAcquisition();
            return;
        }
        LivingEntity selected = TargetSelection.resolveTarget(target);
        if (selected == null || selected != target) {
            resetTargetAcquisition();
            return;
        }
        if (!targetAcquired) {
            targetAcquired = true;
            reactionTimer.reset();
            reactionDelayMillis = (long) reactionDelay.sampleGaussian();
        }
        if (hurtTimeCheck.getValue() && AttackCooldownTracker.hasAttackCooldown(target)) return;
        if (target.isDeadOrDying() || !target.isAlive()) return;
        if (target instanceof Player player && !attackShields.getValue()
                && EntityPredicates.isBlocking(player) && !EntityPredicates.isOutsideBlockingArc(player)) return;
        float tickProgress = (float) sampledAttackCooldown - (float) AttackCooldownTracker.confirmedDamageDelayTicks(target);
        if ((double) client.player.getAttackStrengthScale(tickProgress) < sampledAttackCooldown) return;
        if (!reactionTimer.hasElapsedMillis((double) reactionDelayMillis)) return;
        if (!attackTimer.hasElapsedMillis((double) attackDelayMillis)) return;
        if (lastAttackTick == (long) client.player.tickCount) return;
        if (!allowsCriticalTiming(squaredDistance)) return;
        if (noSweepOrPickHits.getValue() && (EntityRaycasts.wouldSweepAttack() || EntityRaycasts.wouldPickHit())) return;
        event.addClick();
        attackTimer.reset();
        attackDelayMillis = (long) attackDelay.sampleGaussian();
        sampleAttackCooldown();
        lastAttackTick = client.player.tickCount;
    }

    private EntityHitResult findAttackTarget(float distance) {
        EntityHitResult hit;
        if (distance == 0.0f) {
            hit = client.hitResult instanceof EntityHitResult entityHit ? entityHit : null;
        } else {
            hit = EntityRaycasts.raycastEntity(client.player, distance);
        }
        if (hit != null) return hit;
        SpearReachFeature spearReach = (SpearReachFeature) Echo.featureManager.findFeature(SpearReachFeature.class);
        return spearReach == null ? null : spearReach.findExtendedReachTarget();
    }

    private void sampleAttackCooldown() {
        FloatRangeSetting selected = EntityRaycasts.isCriticalState() && !critMode.isSelected(CRIT_NONE)
                ? critCooldown : cooldown;
        sampledAttackCooldown = (double) selected.sampleGaussian() / 100.0;
    }

    private void resetTargetAcquisition() {
        if (targetAcquired) {
            reactionTimer.reset();
            targetAcquired = false;
        }
    }

    private boolean allowsNonCriticalTiming() {
        if (EntityRaycasts.allowsNonCriticalHit()) return true;
        return critOnlyOnSpace.getValue() && !client.options.keyJump.isDown() && !client.player.onGround();
    }

    private boolean allowsCriticalTiming(double squaredDistance) {
        float lower = critRange.getLower();
        float upper = critRange.getUpper();
        if (squaredDistance < (double) (lower * lower) || squaredDistance > (double) (upper * upper)) return true;
        boolean allow = !(critOnlyOnSpace.getValue() && client.options.keyJump.isDown() && client.player.onGround());
        boolean critical = EntityRaycasts.isCriticalState();
        if (critMode.isSelected(CRIT_PRECISE)) {
            if (critical && criticalWindowTimer.hasElapsedMillis(0.0) && !client.player.isSprinting()) {
                criticalWindowTimer.reset();
            } else {
                allow = allowsNonCriticalTiming();
            }
        } else if (critMode.isSelected(CRIT_NORMAL)) {
            if (critical && criticalWindowTimer.hasElapsedMillis(0.0)) {
                criticalWindowTimer.reset();
            } else {
                allow = allowsNonCriticalTiming();
            }
        }
        return allow;
    }

    @Subscribe
    private void onPacketReceive(PacketReceiveEvent event) {
        if (isOutsideWorld()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ClientboundPlayerPositionPacket || packet instanceof ClientboundRespawnPacket) resetTargetAcquisition();
    }

    public static void appendBootstrapSettings(TriggerBotFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, critMode);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, critOnlyOnSpace);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, attackShields);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, hurtTimeCheck);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, noSweepOrPickHits);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, cooldown);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, critCooldown);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, critRange);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, attackDistance);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, reactionDelay);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, attackDelay);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, itemWhitelist);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, hotbarWhitelist);
    }

    public static void assignBootstrapSettings(TriggerBotFeature feature, FloatRangeSetting preparedCooldown,
            ChoiceSetting preparedCritMode, FloatRangeSetting preparedReactionDelay, BooleanSetting preparedCritOnlyOnSpace,
            FloatRangeSetting preparedAttackDelay, ItemListSetting preparedItemWhitelist, FloatSetting preparedAttackDistance,
            BooleanSetting preparedNoSweepOrPickHits, HotbarSlotsSetting preparedHotbarWhitelist,
            BooleanSetting preparedHurtTimeCheck, BooleanSetting preparedAttackShields, FloatRangeSetting preparedCritRange,
            FloatRangeSetting preparedCritCooldown) {
        cooldown = preparedCooldown;
        critMode = preparedCritMode;
        reactionDelay = preparedReactionDelay;
        critOnlyOnSpace = preparedCritOnlyOnSpace;
        attackDelay = preparedAttackDelay;
        itemWhitelist = preparedItemWhitelist;
        attackDistance = preparedAttackDistance;
        noSweepOrPickHits = preparedNoSweepOrPickHits;
        hotbarWhitelist = preparedHotbarWhitelist;
        hurtTimeCheck = preparedHurtTimeCheck;
        attackShields = preparedAttackShields;
        critRange = preparedCritRange;
        critCooldown = preparedCritCooldown;
    }
}
