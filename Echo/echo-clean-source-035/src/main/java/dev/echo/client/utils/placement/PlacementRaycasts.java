package dev.echo.client.utils.placement;

import dev.echo.client.MinecraftAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Structured placement-raycast candidate, subject to the retained cache and guard domains. */
public final class PlacementRaycasts implements MinecraftAccess {
    private static final double HIT_INSET = 1.0E-4;
    private PlacementRaycasts() {
    }

    public static boolean hitsEntity(Vec3 start, Vec3 end) {
        return ProjectileUtil.getEntityHitResult(CLIENT.player, start, end, new AABB(start, end).inflate(1.0),
                Entity::isPickable, start.distanceToSqr(end)) != null;
    }

    public static boolean facesEye(BlockHitResult hit, Vec3 eye) {
        if (hit == null || eye == null || CLIENT.level == null) {
            return false;
        }
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        AABB bounds = collisionBounds(hit.getBlockPos());
        if (bounds == null) {
            return false;
        }
        if (bounds.contains(eye)) {
            return true;
        }
        return switch (hit.getDirection()) {
            case NORTH -> eye.z <= bounds.minZ;
            case SOUTH -> eye.z >= bounds.maxZ;
            case EAST -> eye.x >= bounds.maxX;
            case WEST -> eye.x <= bounds.minX;
            case UP -> eye.y >= bounds.maxY;
            case DOWN -> eye.y <= bounds.minY;
            default -> throw new MatchException(null, null);
        };
    }

    public static AABB collisionBounds(BlockPos position) {
        BlockState state = CLIENT.level.getBlockState(position);
        VoxelShape shape = state.getCollisionShape(CLIENT.level, position);
        return shape.isEmpty() ? new AABB(position) : shape.bounds().move(position);
    }

    public static boolean canSeeHit(Vec3 eye, BlockHitResult expected) {
        Vec3 inside = expected.getLocation().relative(expected.getDirection().getOpposite(), HIT_INSET);
        BlockHitResult actual = CLIENT.level.clip(new ClipContext(eye, inside,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CLIENT.player));
        return actual.getType() == HitResult.Type.BLOCK
                && actual.getBlockPos().equals(expected.getBlockPos())
                && actual.getDirection() == expected.getDirection();
    }

    public static Vec3 pointOnFace(AABB bounds, Direction face, double u, double v) {
        Vec3 center = new Vec3(
                face == Direction.WEST ? bounds.minX : face == Direction.EAST ? bounds.maxX : (bounds.minX + bounds.maxX) * 0.5,
                face == Direction.DOWN ? bounds.minY : face == Direction.UP ? bounds.maxY : (bounds.minY + bounds.maxY) * 0.5,
                face == Direction.NORTH ? bounds.minZ : face == Direction.SOUTH ? bounds.maxZ : (bounds.minZ + bounds.maxZ) * 0.5);
        return switch (face.getAxis()) {
            case X -> center.add(0.0, u * bounds.getYsize(), v * bounds.getZsize());
            case Y -> center.add(u * bounds.getXsize(), 0.0, v * bounds.getZsize());
            case Z -> center.add(u * bounds.getXsize(), v * bounds.getYsize(), 0.0);
            default -> throw new MatchException(null, null);
        };
    }
}
