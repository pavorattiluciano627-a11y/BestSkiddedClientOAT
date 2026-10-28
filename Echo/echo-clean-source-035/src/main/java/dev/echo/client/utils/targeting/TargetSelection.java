package dev.echo.client.utils.targeting;

import static dev.echo.client.MinecraftAccess.CLIENT;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.features.internals.TargetsFeature;
import dev.echo.client.setting.MultiChoiceSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Shared combat-target filtering, retained target selection, and attack history.
 * Binding/cache and removed-control applicability are recorded in this shard's
 * targeting recovery receipt. Settings and world/player state are read live.
 */
public class TargetSelection implements MinecraftAccess {
    private static LivingEntity currentTarget;
    private static LivingEntity lastAttackedTarget;
    private static int lastAttackTick;

    private TargetSelection() {
    }

    /** Records even a currently disallowed living victim before updating the retained target. */
    public static void recordAttack(Entity entity) {
        if (CLIENT.player == null || !(entity instanceof LivingEntity living)) {
            return;
        }
        lastAttackedTarget = living;
        lastAttackTick = CLIENT.player.tickCount;
        if (!isValidTarget(living)) {
            return;
        }
        currentTarget = living;
    }

    /** Applies the original ordered validity, type, player, team, friend and bot checks. */
    public static boolean isValidTarget(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved()
                || entity.isDeadOrDying() || entity.isSpectator() || CLIENT.player == null) {
            return false;
        }
        TargetsFeature targets = targets();
        if (targets == null) {
            return true;
        }
        MultiChoiceSetting types = TargetsFeature.entityTypes;
        boolean hasSelection = false;
        for (Boolean selected : types.getOptions().values()) {
            if (selected) {
                hasSelection = true;
                break;
            }
        }
        if (hasSelection && EntityPredicates.isUnarmored(entity) && !types.isSelected(TargetsFeature.NAKED)) {
            return false;
        }
        if (hasSelection && entity.isInvisible() && !types.isSelected(TargetsFeature.INVISIBLE)) {
            return false;
        }
        if (EntityPredicates.isPlayer(entity)) {
            return isPlayerAllowed(entity, targets, hasSelection);
        }
        if (entity instanceof ArmorStand) {
            return !hasSelection || types.isSelected(TargetsFeature.ARMOR_STANDS);
        }
        if (EntityPredicates.isHostile(entity)) {
            return !hasSelection || types.isSelected(TargetsFeature.HOSTILE);
        }
        if (EntityPredicates.isPassive(entity)) {
            return !hasSelection || types.isSelected(TargetsFeature.PASSIVE);
        }
        if (EntityPredicates.isNeutral(entity)) {
            return !hasSelection || types.isSelected(TargetsFeature.NEUTRAL);
        }
        return !hasSelection;
    }

    /** Resolves the suggestion against keep-target and last-attacked preferences. */
    public static LivingEntity resolveTarget(LivingEntity suggested) {
        if (!isKeepTargetEnabled()) {
            return suggested;
        }
        if (prefersLastAttacked()) {
            return resolveLastAttacked();
        }
        if (canKeepTarget(currentTarget)) {
            return currentTarget;
        }
        if (!canKeepTarget(suggested)) {
            currentTarget = null;
            return null;
        }
        currentTarget = suggested;
        return currentTarget;
    }

    /** Scans world iteration order; target resolution may update shared retained state. */
    public static LivingEntity findNearestTarget(Vec3 origin, float tickDelta) {
        if (origin == null || CLIENT.level == null || CLIENT.player == null) {
            return null;
        }
        LivingEntity nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;
        for (Entity entity : CLIENT.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity == CLIENT.player || !isValidTarget(living)) {
                continue;
            }
            LivingEntity resolved = resolveTarget(living);
            if (resolved == null) {
                continue;
            }
            double distanceSquared = origin.distanceToSqr(resolved.getPosition(tickDelta));
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = resolved;
            }
        }
        return nearest;
    }

    /** Selects only a live, same-world, allowed entity inside the current keep range. */
    public static void selectTarget(LivingEntity entity) {
        if (canKeepTarget(entity)) {
            currentTarget = entity;
        }
    }

    public static void clearTarget() {
        currentTarget = null;
    }

    public static void clearTargets() {
        currentTarget = null;
        lastAttackedTarget = null;
        lastAttackTick = 0;
    }

    /** Inclusive age limit, with the original int subtraction and world validation. */
    public static LivingEntity recentlyAttacked(int maximumAgeTicks) {
        if (CLIENT.player == null || !isLastAttackedValid()) {
            return null;
        }
        if (CLIENT.player.tickCount - lastAttackTick > maximumAgeTicks) {
            return null;
        }
        return lastAttackedTarget;
    }

    public static boolean isBot(LivingEntity entity) {
        TargetsFeature targets = targets();
        return targets != null && isBot(entity, targets);
    }

    /** Whether another dead player is strictly inside the supplied distance. */
    public static boolean hasDeadPlayerWithin(double distance) {
        if (CLIENT.player == null || CLIENT.level == null) {
            return false;
        }
        double distanceSquared = distance * distance;
        for (Player player : CLIENT.level.players()) {
            if (player != CLIENT.player && player.isDeadOrDying()
                    && CLIENT.player.distanceToSqr(player) < distanceSquared) {
                return true;
            }
        }
        return false;
    }

    private static LivingEntity resolveLastAttacked() {
        LivingEntity attacked = validLastAttacked();
        if (attacked != null) {
            currentTarget = attacked;
        }
        if (canKeepTarget(currentTarget)) {
            return currentTarget;
        }
        currentTarget = null;
        return null;
    }

    private static LivingEntity validLastAttacked() {
        return isLastAttackedValid() ? lastAttackedTarget : null;
    }

    private static boolean isLastAttackedValid() {
        if (lastAttackedTarget == null || CLIENT.player == null
                || lastAttackedTarget.level() != CLIENT.level
                || !lastAttackedTarget.isAlive() || lastAttackedTarget.isRemoved()) {
            return false;
        }
        return isValidTarget(lastAttackedTarget);
    }

    private static boolean canKeepTarget(LivingEntity entity) {
        if (entity == null || CLIENT.player == null || !entity.isAlive() || entity.isRemoved()
                || entity.level() != CLIENT.level) {
            return false;
        }
        double range = keepTargetRange();
        if (CLIENT.player.distanceToSqr(entity) > range * range) {
            return false;
        }
        return isValidTarget(entity);
    }

    private static double keepTargetRange() {
        return targets() == null ? 16.0 : TargetsFeature.keepTargetRange.getValue();
    }

    private static boolean isKeepTargetEnabled() {
        return targets() != null && TargetsFeature.keepTarget.getValue();
    }

    private static boolean prefersLastAttacked() {
        return targets() != null && TargetsFeature.keepTarget.getValue() && TargetsFeature.lastAttacked.getValue();
    }

    private static boolean isPlayerAllowed(LivingEntity entity, TargetsFeature targets, boolean hasSelection) {
        if (hasSelection && !TargetsFeature.entityTypes.isSelected(TargetsFeature.PLAYERS)) {
            return false;
        }
        if (TargetsFeature.antiBot.getValue() && isBot(entity, targets)) {
            return false;
        }
        if (TargetsFeature.ignoreTeams.getValue() && targets.isSameTeam(entity)) {
            return false;
        }
        if (TargetsFeature.ignoreFriends.getValue()) {
            String name = entity.getName().getString();
            // Inline only ResultProcessor.resolveQueue's exact friend-manager adapter.
            if (Echo.friendManager != null && Echo.friendManager.contains(name)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBot(LivingEntity entity, TargetsFeature targets) {
        return entity instanceof Player player && CachedPlayerTargets.isBot(player, targets);
    }

    private static TargetsFeature targets() {
        if (Echo.featureManager == null) {
            return null;
        }
        return (TargetsFeature) Echo.featureManager.findFeature(TargetsFeature.class);
    }
}
