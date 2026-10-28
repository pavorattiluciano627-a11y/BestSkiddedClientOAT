package dev.echo.client.utils.targeting;

import static dev.echo.client.MinecraftAccess.CLIENT;

import dev.echo.client.features.internals.TargetsFeature;
import dev.echo.client.handlers.BlockingTicksTracker;
import dev.echo.client.handlers.LatencyTracker;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

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
    public static boolean isUsingBlockingItem(PlayerEntity player) {
        if (player == null || !player.isUsingItem()) {
            return false;
        }
        ItemStack activeItem = player.getActiveItem();
        if (activeItem.isEmpty()) {
            return false;
        }
        return activeItem.getItem().getUseAction(activeItem) == UseAction.BLOCK;
    }

    /** Applies the current shield-detection mode, delay and ping compensation. */
    public static boolean isBlocking(PlayerEntity player) {
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
    public static boolean isOutsideBlockingArc(PlayerEntity player) {
        if (CLIENT.player == null || player == null) {
            return false;
        }
        Vec3d localPosition = CLIENT.player.getEntityPos();
        Vec3d playerPosition = player.getEntityPos();
        Vec3d direction = localPosition.subtract(playerPosition);
        direction = new Vec3d(direction.x, 0.0, direction.z);
        if (direction.lengthSquared() == 0.0) {
            return false;
        }
        direction = direction.normalize();
        Vec3d facing = player.getRotationVector(0.0F, player.getHeadYaw());
        double angle = Math.acos(MathHelper.clamp(direction.dotProduct(facing), -1.0, 1.0));

        BlocksAttacksComponent blocking = player.getActiveItem().get(DataComponentTypes.BLOCKS_ATTACKS);
        if (blocking == null) {
            ItemStack heldItem = player.getStackInHand(player.getActiveHand());
            blocking = heldItem.get(DataComponentTypes.BLOCKS_ATTACKS);
        }
        double maximumAngleDegrees = 90.0;
        if (blocking != null && !blocking.damageReductions().isEmpty()) {
            maximumAngleDegrees = 0.0;
            for (BlocksAttacksComponent.DamageReduction reduction : blocking.damageReductions()) {
                maximumAngleDegrees = Math.max(maximumAngleDegrees, reduction.horizontalBlockingAngle());
            }
        }
        return angle > (Math.PI / 180.0) * maximumAngleDegrees;
    }

    /** Nearest other non-dead, non-spectator player; ties keep list order. */
    public static PlayerEntity nearestPlayer() {
        if (CLIENT.player == null || CLIENT.world == null) {
            return null;
        }
        PlayerEntity nearest = null;
        double nearestSquaredDistance = Double.MAX_VALUE;
        for (PlayerEntity player : CLIENT.world.getPlayers()) {
            if (player == CLIENT.player || player.isDead() || player.isSpectator()) {
                continue;
            }
            double squaredDistance = CLIENT.player.squaredDistanceTo(player);
            if (squaredDistance < nearestSquaredDistance) {
                nearestSquaredDistance = squaredDistance;
                nearest = player;
            }
        }
        return nearest;
    }

    /** Nearest other non-dead living entity in the expanded player bounding box. */
    public static LivingEntity nearestLivingEntity(int range) {
        if (CLIENT.player == null || CLIENT.world == null) {
            return null;
        }
        LivingEntity nearest = null;
        double nearestSquaredDistance = Double.MAX_VALUE;
        Box searchBox = CLIENT.player.getBoundingBox().expand((double) range);
        for (LivingEntity entity : CLIENT.world.getEntitiesByClass(
                LivingEntity.class, searchBox, EntityPredicates::isLivingCandidate)) {
            double squaredDistance = CLIENT.player.squaredDistanceTo(entity);
            if (squaredDistance < nearestSquaredDistance) {
                nearestSquaredDistance = squaredDistance;
                nearest = entity;
            }
        }
        return nearest;
    }

    private static boolean isLivingCandidate(LivingEntity entity) {
        return entity != CLIENT.player && !entity.isDead();
    }

    /** Signed local-player Y minus the other player's Y. */
    public static double verticalDifference(PlayerEntity player) {
        if (CLIENT.player == null || player == null) {
            return 0.0;
        }
        return CLIENT.player.getY() - player.getY();
    }

    public static double horizontalDistance(PlayerEntity player) {
        if (CLIENT.player == null || player == null) {
            return 0.0;
        }
        double x = CLIENT.player.getX() - player.getX();
        double z = CLIENT.player.getZ() - player.getZ();
        return Math.sqrt(x * x + z * z);
    }

    public static double distanceTo(PlayerEntity player) {
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
        if (CLIENT.player == null || CLIENT.world == null) {
            return 0.0;
        }
        double playerY = CLIENT.player.getY();
        for (int y = (int) Math.floor(playerY); y >= 0; y--) {
            if (!CLIENT.world.getBlockState(CLIENT.player.getBlockPos().withY(y)).isAir()) {
                return playerY - y - 1.0;
            }
        }
        return playerY;
    }

    public static boolean isPlayer(Entity entity) {
        return entity instanceof PlayerEntity;
    }

    public static boolean isHostile(Entity entity) {
        if (entity instanceof HostileEntity) {
            return !(entity instanceof ZombifiedPiglinEntity piglin) || piglin.isAttacking();
        }
        if (entity instanceof SlimeEntity slime && slime.isSmall()) {
            return false;
        }
        return entity instanceof Monster || entity instanceof MobEntity mob && mob.isAttacking();
    }

    public static boolean isPassive(Entity entity) {
        return entity instanceof PassiveEntity;
    }

    public static boolean isNeutral(Entity entity) {
        return entity instanceof MobEntity && !isHostile(entity) && !isPassive(entity);
    }

    /** No armor in head/legs/feet, and an empty chest slot or an elytra. */
    public static boolean isUnarmored(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        ItemStack chest = entity.getEquippedStack(EquipmentSlot.CHEST);
        if (!entity.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
            return false;
        }
        if (!chest.isEmpty() && chest.getItem() != Items.ELYTRA) {
            return false;
        }
        return entity.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
                && entity.getEquippedStack(EquipmentSlot.FEET).isEmpty();
    }
}
