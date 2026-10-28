package dev.echo.client.utils.targeting;

import static dev.echo.client.MinecraftAccess.CLIENT;

import dev.echo.client.features.internals.TargetsFeature;
import dev.echo.client.handlers.BlockingTicksTracker;
import dev.echo.client.handlers.LatencyTracker;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Entity classification, proximity queries and shield-state detection.
 *
 * <p>Reconstructed ordinary-source candidate. Original member bindings and
 * initialization/control-state applicability are recorded in the accompanying
 * entity-predicates receipt. World, player, equipment and settings are read live.
 */
public class EntityPredicates {
    private static final int LATENCY_GRACE_MILLIS = 25;
    private static final int MILLIS_PER_TICK = 50;

    public EntityPredicates() {}

    /** Whether the actively used item's use action is blocking. */
    public static boolean isUsingBlockingItem(Player player) {
        if (player == null || !player.isUsingItem()) {
            return false;
        }
        ItemStack activeItem = player.getUseItem();
        if (activeItem.isEmpty()) {
            return false;
        }
        return activeItem.getItem().getUseAnimation(activeItem) == ItemUseAnimation.BLOCK;
    }

    /** Applies the current shield-detection mode, delay and ping compensation. */
    public static boolean isBlocking(Player player) {
        if (player == null) {
            return false;
        }
        if (TargetsFeature.usesVanillaShieldDetection()) {
            return player.isBlocking();
        }
        int requiredTicks = TargetsFeature.shieldTicks.getValue();
        if (TargetsFeature.pingCompensation.getValue()) {
            requiredTicks -= Math.max(0, LatencyTracker.milliseconds() - LATENCY_GRACE_MILLIS)
                    / MILLIS_PER_TICK;
        }
        return BlockingTicksTracker.ticksBlocking(player) >= Math.max(requiredTicks, 1);
    }

    /** Whether the local player's direction lies outside this player's shield arc. */
    public static boolean isOutsideBlockingArc(Player player) {
        if (CLIENT.player == null || player == null) {
            return false;
        }
        Vec3 localPosition = CLIENT.player.position();
        Vec3 playerPosition = player.position();
        Vec3 direction = localPosition.subtract(playerPosition);
        direction = new Vec3(direction.x, 0.0, direction.z);
        if (direction.lengthSqr() == 0.0) {
            return false;
        }
        direction = direction.normalize();
        Vec3 facing = player.calculateViewVector(0.0F, player.getYHeadRot());
        double angle = Math.acos(Mth.clamp(direction.dot(facing), -1.0, 1.0));

        BlocksAttacks blocking = player.getUseItem().get(DataComponents.BLOCKS_ATTACKS);
        if (blocking == null) {
            ItemStack heldItem = player.getItemInHand(player.getUsedItemHand());
            blocking = heldItem.get(DataComponents.BLOCKS_ATTACKS);
        }
        double maximumAngleDegrees = 90.0;
        if (blocking != null && !blocking.damageReductions().isEmpty()) {
            maximumAngleDegrees = 0.0;
            for (BlocksAttacks.DamageReduction reduction : blocking.damageReductions()) {
                maximumAngleDegrees = Math.max(maximumAngleDegrees, reduction.horizontalBlockingAngle());
            }
        }
        return angle > (Math.PI / 180.0) * maximumAngleDegrees;
    }

    /** Nearest other non-dead, non-spectator player; ties keep list order. */
    public static Player nearestPlayer() {
        if (CLIENT.player == null || CLIENT.level == null) {
            return null;
        }
        Player nearest = null;
        double nearestSquaredDistance = Double.MAX_VALUE;
        for (Player player : CLIENT.level.players()) {
            if (player == CLIENT.player || player.isDeadOrDying() || player.isSpectator()) {
                continue;
            }
            double squaredDistance = CLIENT.player.distanceToSqr(player);
            if (squaredDistance < nearestSquaredDistance) {
                nearestSquaredDistance = squaredDistance;
                nearest = player;
            }
        }
        return nearest;
    }

    /** Nearest other non-dead living entity in the expanded player bounding box. */
    public static LivingEntity nearestLivingEntity(int range) {
        if (CLIENT.player == null || CLIENT.level == null) {
            return null;
        }
        LivingEntity nearest = null;
        double nearestSquaredDistance = Double.MAX_VALUE;
        AABB searchBox = CLIENT.player.getBoundingBox().inflate((double) range);
        for (LivingEntity entity : CLIENT.level.getEntitiesOfClass(
                LivingEntity.class, searchBox, EntityPredicates::isLivingCandidate)) {
            double squaredDistance = CLIENT.player.distanceToSqr(entity);
            if (squaredDistance < nearestSquaredDistance) {
                nearestSquaredDistance = squaredDistance;
                nearest = entity;
            }
        }
        return nearest;
    }

    private static boolean isLivingCandidate(LivingEntity entity) {
        return entity != CLIENT.player && !entity.isDeadOrDying();
    }

    /** Signed local-player Y minus the other player's Y. */
    public static double verticalDifference(Player player) {
        if (CLIENT.player == null || player == null) {
            return 0.0;
        }
        return CLIENT.player.getY() - player.getY();
    }

    public static double horizontalDistance(Player player) {
        if (CLIENT.player == null || player == null) {
            return 0.0;
        }
        double x = CLIENT.player.getX() - player.getX();
        double z = CLIENT.player.getZ() - player.getZ();
        return Math.sqrt(x * x + z * z);
    }

    public static double distanceTo(Player player) {
        if (CLIENT.player == null || player == null) {
            return 0.0;
        }
        double x = CLIENT.player.getX() - player.getX();
        double y = CLIENT.player.getY() - player.getY();
        double z = CLIENT.player.getZ() - player.getZ();
        return Math.sqrt(x * x + y * y + z * z);
    }

    /**
     * Distance above the first non-air block down to Y=0, or player Y if none.
     * The original stops at zero and can return a negative distance inside a block.
     */
    public static double heightAboveGround() {
        if (CLIENT.player == null || CLIENT.level == null) {
            return 0.0;
        }
        double playerY = CLIENT.player.getY();
        for (int y = (int) Math.floor(playerY); y >= 0; y--) {
            if (!CLIENT.level.getBlockState(CLIENT.player.blockPosition().atY(y)).isAir()) {
                return playerY - y - 1.0;
            }
        }
        return playerY;
    }

    public static boolean isPlayer(Entity entity) {
        return entity instanceof Player;
    }

    public static boolean isHostile(Entity entity) {
        if (entity instanceof Monster) {
            return !(entity instanceof ZombifiedPiglin piglin) || piglin.isAggressive();
        }
        if (entity instanceof Slime slime && slime.isTiny()) {
            return false;
        }
        return entity instanceof Enemy || entity instanceof Mob mob && mob.isAggressive();
    }

    public static boolean isPassive(Entity entity) {
        return entity instanceof AgeableMob;
    }

    public static boolean isNeutral(Entity entity) {
        return entity instanceof Mob && !isHostile(entity) && !isPassive(entity);
    }

    /** No armor in head/legs/feet, and an empty chest slot or an elytra. */
    public static boolean isUnarmored(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (!entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            return false;
        }
        if (!chest.isEmpty() && chest.getItem() != Items.ELYTRA) {
            return false;
        }
        return entity.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                && entity.getItemBySlot(EquipmentSlot.FEET).isEmpty();
    }
}
