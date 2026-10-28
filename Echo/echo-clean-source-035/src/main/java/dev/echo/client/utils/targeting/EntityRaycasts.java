package dev.echo.client.utils.targeting;

import static dev.echo.client.MinecraftAccess.CLIENT;
import static dev.echo.client.bootstrap.CatalogText.literal;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.handlers.AttackCooldownTracker;
import dev.echo.client.setting.ChoiceSetting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Entity hit queries, target-priority ordering and attack-state predicates.
 * The accompanying recovery receipt retains original binding, key/control and
 * initialization applicability. Queries use live game state and real hit results.
 */
public final class EntityRaycasts implements MinecraftAccess {
    private static final CharSequence HURT_TICK = literal("HurtTick");
    private static final CharSequence FOV = literal("FOV");
    private static final AttackRange DEFAULT_STACK_RANGE =
            new AttackRange(2.0F, 4.5F, 2.0F, 6.5F, 0.125F, 0.5F);

    private EntityRaycasts() {
    }

    public static boolean hasNormalReachTarget(Player player) {
        return normalReachTarget(player) != null;
    }

    /** Uses the stack component or the client's original explicit fallback component. */
    public static EntityHitResult raycastWithStackRange(Player player, ItemStack stack) {
        AttackRange range = stack.get(DataComponents.ATTACK_RANGE);
        return raycastWithRange(player, range != null ? range : DEFAULT_STACK_RANGE);
    }

    /**
     * Reuses an existing living-entity crosshair hit inside range. A block crosshair
     * hit stops the query. Otherwise the outline raycast limits the entity ray.
     */
    public static EntityHitResult raycastEntity(Player player, double range) {
        if (player == null || !player.isAlive() || CLIENT.level == null) {
            return null;
        }
        HitResult crosshair = CLIENT.hitResult;
        if (crosshair instanceof EntityHitResult entityHit) {
            if (!(entityHit.getEntity() instanceof LivingEntity)) {
                return null;
            }
            double distanceSquared = player.getEyePosition().distanceToSqr(entityHit.getLocation());
            return distanceSquared <= range * range ? entityHit : null;
        }
        if (CLIENT.hitResult != null && CLIENT.hitResult.getType() == HitResult.Type.BLOCK) {
            return null;
        }
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(range));
        BlockHitResult blockHit = CLIENT.level.clip(new ClipContext(start, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 limitedEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        return ProjectileUtil.getEntityHitResult(player, start, limitedEnd,
                player.getBoundingBox().expandTowards(limitedEnd.subtract(start)).inflate(1.0),
                EntityRaycasts::canRaycastLivingEntity, range * range);
    }

    public static LivingEntity normalReachTarget(Player player) {
        if (player == null || !player.isAlive()) {
            return null;
        }
        ItemStack stack = player.getMainHandItem();
        AttackRange range = stack.isEmpty() ? null : stack.get(DataComponents.ATTACK_RANGE);
        if (range == null) {
            range = AttackRange.defaultFor(player);
        }
        EntityHitResult hit = raycastWithRange(player, range);
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    private static EntityHitResult raycastWithRange(Player player, AttackRange range) {
        if (player == null || range == null || !player.isAlive()) {
            return null;
        }
        HitResult hit = range.getClosesetHit(player, 1.0F, net.minecraft.world.entity.EntitySelector.CAN_BE_PICKED);
        if (!(hit instanceof EntityHitResult entityHit) || !range.isInRange(player, entityHit.getLocation())) {
            return null;
        }
        if (!(entityHit.getEntity() instanceof LivingEntity living) || !living.isAlive() || living.isRemoved()) {
            return null;
        }
        return entityHit;
    }

    /** Horizontal field of view; the full supplied angle is halved at the comparison. */
    public static boolean isWithinFov(Player player, LivingEntity target, float fullYawFov) {
        if (player == null || target == null) {
            return false;
        }
        Vec3 offset = target.getEyePosition().subtract(player.getEyePosition());
        double targetYaw = Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90.0;
        double yawDifference = Math.abs(Mth.wrapDegrees((float) targetYaw - player.getYRot()));
        return yawDifference <= (double) (fullYawFov / 2.0F);
    }

    /** Separate full yaw and pitch fields of view, both inclusive at their boundaries. */
    public static boolean isWithinFov(Player player, LivingEntity target, float fullYawFov, float fullPitchFov) {
        if (player == null || target == null) {
            return false;
        }
        Vec3 offset = target.getEyePosition().subtract(player.getEyePosition());
        double targetYaw = Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90.0;
        double horizontalDistance = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        double targetPitch = -Math.toDegrees(Math.atan2(offset.y, horizontalDistance));
        double yawDifference = Math.abs(Mth.wrapDegrees((float) targetYaw - player.getYRot()));
        double pitchDifference = Math.abs((float) targetPitch - player.getXRot());
        return yawDifference <= (double) (fullYawFov / 2.0F)
                && pitchDifference <= (double) (fullPitchFov / 2.0F);
    }

    /** Nearest feet-position distance; equal scores preserve source-list order. */
    public static LivingEntity nearestTarget(Player player, List<LivingEntity> candidates) {
        if (player == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    /** Original HurtTick, FOV, then nearest priority with the original tie breakers. */
    public static LivingEntity selectTarget(Player player, List<LivingEntity> candidates,
                                            Vec3 origin, ChoiceSetting priority) {
        if (player == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (priority.isSelected(HURT_TICK)) {
            return candidates.stream().min(Comparator.comparingLong(AttackCooldownTracker::lastAttackTick)
                    .thenComparingDouble(entity -> squaredDistanceToEntity(origin, entity))
                    .thenComparingDouble(entity -> angularDistance(player, entity))).orElse(null);
        }
        if (priority.isSelected(FOV)) {
            return candidates.stream().min(Comparator.comparingDouble((LivingEntity entity) -> angularDistance(player, entity))
                    .thenComparingDouble(entity -> squaredDistanceToEntity(origin, entity))).orElse(null);
        }
        return nearestTarget(player, candidates);
    }

    private static double squaredDistanceToEntity(Vec3 origin, LivingEntity entity) {
        return origin.distanceToSqr(entity.position());
    }

    private static double angularDistance(Player player, LivingEntity target) {
        float tickProgress = CLIENT.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 offset = target.getEyePosition(tickProgress).subtract(player.getEyePosition(tickProgress));
        double horizontalDistance = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(offset.y, horizontalDistance));
        float yawDifference = Math.abs(Mth.wrapDegrees(targetYaw - player.getYRot()));
        float pitchDifference = Math.abs(targetPitch - player.getXRot());
        return Math.sqrt(yawDifference * yawDifference + pitchDifference * pitchDifference);
    }

    /** Airborne falling state, retaining every original vehicle/fluid/effect gate. */
    public static boolean isCriticalState() {
        return CLIENT.player != null && !CLIENT.player.onGround() && !CLIENT.player.isPassenger()
                && !CLIENT.player.onClimbable() && !CLIENT.player.isInWater()
                && !CLIENT.player.hasEffect(MobEffects.BLINDNESS) && !CLIENT.player.isFallFlying()
                && CLIENT.player.fallDistance > 0.0;
    }

    public static boolean wouldSweepAttack() {
        if (CLIENT.player == null || CLIENT.player.getAttackStrengthScale(0.5F) <= 0.9F
                || CLIENT.player.isSprinting() || !CLIENT.player.onGround()
                || !CLIENT.player.getMainHandItem().is(ItemTags.SWORDS)) {
            return false;
        }
        double x = CLIENT.player.getX() - CLIENT.player.xo;
        double z = CLIENT.player.getZ() - CLIENT.player.zo;
        double speed = (double) CLIENT.player.getSpeed() * 2.5;
        return x * x + z * z < Mth.square(speed);
    }

    public static boolean wouldPickHit() {
        if (CLIENT.player == null || CLIENT.player.getAttackStrengthScale(0.5F) > 0.9F) {
            return false;
        }
        double x = CLIENT.player.getX() - CLIENT.player.xo;
        double z = CLIENT.player.getZ() - CLIENT.player.zo;
        return x * x + z * z <= 1.0E-5;
    }

    public static boolean allowsNonCriticalHit() {
        if (CLIENT.player == null) {
            return false;
        }
        return CLIENT.player.onGround() || CLIENT.player.isFallFlying() || CLIENT.player.isInWater()
                || CLIENT.player.onClimbable() || intersectsCobweb() || intersectsSweetBerryBush();
    }

    private static boolean canRaycastLivingEntity(Entity entity) {
        return entity instanceof LivingEntity && net.minecraft.world.entity.EntitySelector.CAN_BE_PICKED.test(entity);
    }

    // Exact ConnectionRegistry query slices, inlined instead of adding a fake provider.
    private static boolean intersectsCobweb() {
        AABB bounds = CLIENT.player.getBoundingBox();
        BlockPos origin = BlockPos.containing(CLIENT.player.position());
        return nearbyPositions(origin).stream().anyMatch(position -> bounds.intersects(new AABB(position))
                && CLIENT.level.getBlockState(position).getBlock() == Blocks.COBWEB);
    }

    private static boolean intersectsSweetBerryBush() {
        AABB bounds = CLIENT.player.getBoundingBox();
        BlockPos origin = BlockPos.containing(CLIENT.player.position());
        return nearbyPositions(origin).stream().anyMatch(position -> bounds.intersects(new AABB(position))
                && CLIENT.level.getBlockState(position).getBlock() == Blocks.SWEET_BERRY_BUSH);
    }

    private static List<BlockPos> nearbyPositions(BlockPos origin) {
        List<BlockPos> positions = new ArrayList<>();
        for (int x = origin.getX() - 2; x <= origin.getX() + 2; x++) {
            for (int y = origin.getY() - 1; y <= origin.getY() + 4; y++) {
                for (int z = origin.getZ() - 2; z <= origin.getZ() + 2; z++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
        return positions;
    }
}
