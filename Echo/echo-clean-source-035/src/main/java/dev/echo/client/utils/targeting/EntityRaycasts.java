package dev.echo.client.utils.targeting;

import static dev.echo.client.MinecraftAccess.CLIENT;
import static dev.echo.client.bootstrap.CatalogText.literal;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.handlers.AttackCooldownTracker;
import dev.echo.client.setting.ChoiceSetting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Entity hit queries, target-priority ordering and attack-state predicates.
 * The accompanying recovery receipt retains original binding, key/control and
 * initialization applicability. Queries use live game state and real hit results.
 */
public final class EntityRaycasts implements MinecraftAccess {
    private static final CharSequence HURT_TICK = literal("HurtTick");
    private static final CharSequence FOV = literal("FOV");
    private static final AttackRangeComponent DEFAULT_STACK_RANGE =
            new AttackRangeComponent(2.0F, 4.5F, 2.0F, 6.5F, 0.125F, 0.5F);

    private EntityRaycasts() {
    }

    public static boolean hasNormalReachTarget(PlayerEntity player) {
        return normalReachTarget(player) != null;
    }

    /** Uses the stack component or the client's original explicit fallback component. */
    public static EntityHitResult raycastWithStackRange(PlayerEntity player, ItemStack stack) {
        AttackRangeComponent range = stack.get(DataComponentTypes.ATTACK_RANGE);
        return raycastWithRange(player, range != null ? range : DEFAULT_STACK_RANGE);
    }

    /**
     * Reuses an existing living-entity crosshair hit inside range. A block crosshair
     * hit stops the query. Otherwise the outline raycast limits the entity ray.
     */
    public static EntityHitResult raycastEntity(PlayerEntity player, double range) {
        if (player == null || !player.isAlive() || CLIENT.world == null) {
            return null;
        }
        HitResult crosshair = CLIENT.crosshairTarget;
        if (crosshair instanceof EntityHitResult entityHit) {
            if (!(entityHit.getEntity() instanceof LivingEntity)) {
                return null;
            }
            double distanceSquared = player.getEyePos().squaredDistanceTo(entityHit.getPos());
            return distanceSquared <= range * range ? entityHit : null;
        }
        if (CLIENT.crosshairTarget != null && CLIENT.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            return null;
        }
        Vec3d start = player.getEyePos();
        Vec3d end = start.add(player.getRotationVec(1.0F).multiply(range));
        BlockHitResult blockHit = CLIENT.world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
        Vec3d limitedEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getPos();
        return ProjectileUtil.raycast(player, start, limitedEnd,
                player.getBoundingBox().stretch(limitedEnd.subtract(start)).expand(1.0),
                EntityRaycasts::canRaycastLivingEntity, range * range);
    }

    public static LivingEntity normalReachTarget(PlayerEntity player) {
        if (player == null || !player.isAlive()) {
            return null;
        }
        ItemStack stack = player.getMainHandStack();
        AttackRangeComponent range = stack.isEmpty() ? null : stack.get(DataComponentTypes.ATTACK_RANGE);
        if (range == null) {
            range = AttackRangeComponent.defaultForEntity(player);
        }
        EntityHitResult hit = raycastWithRange(player, range);
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    private static EntityHitResult raycastWithRange(PlayerEntity player, AttackRangeComponent range) {
        if (player == null || range == null || !player.isAlive()) {
            return null;
        }
        HitResult hit = range.getHitResult(player, 1.0F, net.minecraft.predicate.entity.EntityPredicates.CAN_HIT);
        if (!(hit instanceof EntityHitResult entityHit) || !range.isWithinRange(player, entityHit.getPos())) {
            return null;
        }
        if (!(entityHit.getEntity() instanceof LivingEntity living) || !living.isAlive() || living.isRemoved()) {
            return null;
        }
        return entityHit;
    }

    /** Horizontal field of view; the full supplied angle is halved at the comparison. */
    public static boolean isWithinFov(PlayerEntity player, LivingEntity target, float fullYawFov) {
        if (player == null || target == null) {
            return false;
        }
        Vec3d offset = target.getEyePos().subtract(player.getEyePos());
        double targetYaw = Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90.0;
        double yawDifference = Math.abs(MathHelper.wrapDegrees((float) targetYaw - player.getYaw()));
        return yawDifference <= (double) (fullYawFov / 2.0F);
    }

    /** Separate full yaw and pitch fields of view, both inclusive at their boundaries. */
    public static boolean isWithinFov(PlayerEntity player, LivingEntity target, float fullYawFov, float fullPitchFov) {
        if (player == null || target == null) {
            return false;
        }
        Vec3d offset = target.getEyePos().subtract(player.getEyePos());
        double targetYaw = Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90.0;
        double horizontalDistance = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        double targetPitch = -Math.toDegrees(Math.atan2(offset.y, horizontalDistance));
        double yawDifference = Math.abs(MathHelper.wrapDegrees((float) targetYaw - player.getYaw()));
        double pitchDifference = Math.abs((float) targetPitch - player.getPitch());
        return yawDifference <= (double) (fullYawFov / 2.0F)
                && pitchDifference <= (double) (fullPitchFov / 2.0F);
    }

    /** Nearest feet-position distance; equal scores preserve source-list order. */
    public static LivingEntity nearestTarget(PlayerEntity player, List<LivingEntity> candidates) {
        if (player == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.stream().min(Comparator.comparingDouble(player::squaredDistanceTo)).orElse(null);
    }

    /** Original HurtTick, FOV, then nearest priority with the original tie breakers. */
    public static LivingEntity selectTarget(PlayerEntity player, List<LivingEntity> candidates,
                                            Vec3d origin, ChoiceSetting priority) {
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

    private static double squaredDistanceToEntity(Vec3d origin, LivingEntity entity) {
        return origin.squaredDistanceTo(entity.getEntityPos());
    }

    private static double angularDistance(PlayerEntity player, LivingEntity target) {
        float tickProgress = CLIENT.getRenderTickCounter().getTickProgress(false);
        Vec3d offset = target.getCameraPosVec(tickProgress).subtract(player.getCameraPosVec(tickProgress));
        double horizontalDistance = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(offset.y, horizontalDistance));
        float yawDifference = Math.abs(MathHelper.wrapDegrees(targetYaw - player.getYaw()));
        float pitchDifference = Math.abs(targetPitch - player.getPitch());
        return Math.sqrt(yawDifference * yawDifference + pitchDifference * pitchDifference);
    }

    /** Airborne falling state, retaining every original vehicle/fluid/effect gate. */
    public static boolean isCriticalState() {
        return CLIENT.player != null && !CLIENT.player.isOnGround() && !CLIENT.player.hasVehicle()
                && !CLIENT.player.isClimbing() && !CLIENT.player.isTouchingWater()
                && !CLIENT.player.hasStatusEffect(StatusEffects.BLINDNESS) && !CLIENT.player.isGliding()
                && CLIENT.player.fallDistance > 0.0;
    }

    public static boolean wouldSweepAttack() {
        if (CLIENT.player == null || CLIENT.player.getAttackCooldownProgress(0.5F) <= 0.9F
                || CLIENT.player.isSprinting() || !CLIENT.player.isOnGround()
                || !CLIENT.player.getMainHandStack().isIn(ItemTags.SWORDS)) {
            return false;
        }
        double x = CLIENT.player.getX() - CLIENT.player.lastX;
        double z = CLIENT.player.getZ() - CLIENT.player.lastZ;
        double speed = (double) CLIENT.player.getMovementSpeed() * 2.5;
        return x * x + z * z < MathHelper.square(speed);
    }

    public static boolean wouldPickHit() {
        if (CLIENT.player == null || CLIENT.player.getAttackCooldownProgress(0.5F) > 0.9F) {
            return false;
        }
        double x = CLIENT.player.getX() - CLIENT.player.lastX;
        double z = CLIENT.player.getZ() - CLIENT.player.lastZ;
        return x * x + z * z <= 1.0E-5;
    }

    public static boolean allowsNonCriticalHit() {
        if (CLIENT.player == null) {
            return false;
        }
        return CLIENT.player.isOnGround() || CLIENT.player.isGliding() || CLIENT.player.isTouchingWater()
                || CLIENT.player.isClimbing() || intersectsCobweb() || intersectsSweetBerryBush();
    }

    private static boolean canRaycastLivingEntity(Entity entity) {
        return entity instanceof LivingEntity && net.minecraft.predicate.entity.EntityPredicates.CAN_HIT.test(entity);
    }

    // Exact ConnectionRegistry query slices, inlined instead of adding a fake provider.
    private static boolean intersectsCobweb() {
        Box bounds = CLIENT.player.getBoundingBox();
        BlockPos origin = BlockPos.ofFloored(CLIENT.player.getEntityPos());
        return nearbyPositions(origin).stream().anyMatch(position -> bounds.intersects(new Box(position))
                && CLIENT.world.getBlockState(position).getBlock() == Blocks.COBWEB);
    }

    private static boolean intersectsSweetBerryBush() {
        Box bounds = CLIENT.player.getBoundingBox();
        BlockPos origin = BlockPos.ofFloored(CLIENT.player.getEntityPos());
        return nearbyPositions(origin).stream().anyMatch(position -> bounds.intersects(new Box(position))
                && CLIENT.world.getBlockState(position).getBlock() == Blocks.SWEET_BERRY_BUSH);
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
