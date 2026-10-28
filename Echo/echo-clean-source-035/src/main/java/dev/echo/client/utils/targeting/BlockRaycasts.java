package dev.echo.client.utils.targeting;

import static dev.echo.client.MinecraftAccess.CLIENT;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Block-interaction rays and rotation-aware hit matching. The world-04 receipt
 * records the original binding, opaque-control and initialization conditions.
 */
public final class BlockRaycasts implements MinecraftAccess {
    private BlockRaycasts() {
    }

    /** Casts from the local player's camera at the end of the tick. */
    public static BlockHitResult raycast(float yaw, float pitch) {
        if (CLIENT.player == null || CLIENT.level == null) {
            return null;
        }
        return raycast(CLIENT.player.getEyePosition(1.0F), yaw, pitch);
    }

    /** Uses the player's block interaction range, outline shapes, and no fluids. */
    public static BlockHitResult raycast(Vec3 origin, float yaw, float pitch) {
        if (CLIENT.player == null || CLIENT.level == null) {
            return null;
        }
        Vec3 end = origin.add(CLIENT.player.calculateViewVector(pitch, yaw)
                .scale(CLIENT.player.blockInteractionRange()));
        return CLIENT.level.clip(new ClipContext(origin, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CLIENT.player));
    }

    /** Casts to an explicit endpoint from the interpolated local-player camera. */
    public static HitResult raycastTo(Vec3 end, float tickDelta) {
        if (CLIENT.player == null || CLIENT.level == null) {
            return null;
        }
        Vec3 origin = CLIENT.player.getEyePosition(tickDelta);
        return CLIENT.level.clip(new ClipContext(origin, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CLIENT.player));
    }

    /**
     * Requires the effective rotation to hit the requested block, optionally its
     * exact face, with no hittable entity between the camera and that block hit.
     * The two rotation lookups and original null/error behavior are retained.
     */
    public static boolean matchesHit(BlockHitResult expected, boolean requireSameSide) {
        BlockHitResult actual = raycast(
                Echo.handlerManager.getRotationManager().effectiveRotation().yaw(),
                Echo.handlerManager.getRotationManager().effectiveRotation().pitch());
        if (actual == null || actual.getType() != HitResult.Type.BLOCK
                || !actual.getBlockPos().equals(expected.getBlockPos())) {
            return false;
        }
        if (requireSameSide && actual.getDirection() != expected.getDirection()) {
            return false;
        }
        Vec3 origin = CLIENT.player.getEyePosition(1.0F);
        return ProjectileUtil.getEntityHitResult(CLIENT.player, origin, actual.getLocation(),
                new AABB(origin, actual.getLocation()).inflate(1.0), Entity::isPickable,
                origin.distanceToSqr(actual.getLocation())) == null;
    }

    /** Geometric reach test against a slightly expanded unit block box. */
    public static boolean intersectsBlock(float yaw, float pitch, BlockPos position) {
        if (CLIENT.player == null || position == null) {
            return false;
        }
        Vec3 origin = CLIENT.player.getEyePosition(1.0F);
        Vec3 direction = CLIENT.player.calculateViewVector(pitch, yaw);
        Vec3 end = origin.add(direction.scale(CLIENT.player.blockInteractionRange()));
        return new AABB(position).inflate(1.0E-4).clip(origin, end).isPresent();
    }
}
