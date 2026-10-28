package dev.echo.client.utils.placement;

import dev.echo.client.MinecraftAccess;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;

/** Structured placement-raycast candidate, subject to the retained cache and guard domains. */
public final class PlacementRaycasts implements MinecraftAccess {
    private static final double HIT_INSET = 1.0E-4;
    private PlacementRaycasts() {
    }

    public static boolean hitsEntity(Vec3d start, Vec3d end) {
        return ProjectileUtil.raycast(CLIENT.player, start, end, new Box(start, end).expand(1.0),
                Entity::canHit, start.squaredDistanceTo(end)) != null;
    }

    public static boolean facesEye(BlockHitResult hit, Vec3d eye) {
        if (hit == null || eye == null || CLIENT.world == null) {
            return false;
        }
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        Box bounds = collisionBounds(hit.getBlockPos());
        if (bounds == null) {
            return false;
        }
        if (bounds.contains(eye)) {
            return true;
        }
        return switch (hit.getSide()) {
            case NORTH -> eye.z <= bounds.minZ;
            case SOUTH -> eye.z >= bounds.maxZ;
            case EAST -> eye.x >= bounds.maxX;
            case WEST -> eye.x <= bounds.minX;
            case UP -> eye.y >= bounds.maxY;
            case DOWN -> eye.y <= bounds.minY;
            default -> throw new MatchException(null, null);
        };
    }

    public static Box collisionBounds(BlockPos position) {
        BlockState state = CLIENT.world.getBlockState(position);
        VoxelShape shape = state.getCollisionShape(CLIENT.world, position);
        return shape.isEmpty() ? new Box(position) : shape.getBoundingBox().offset(position);
    }

    public static boolean canSeeHit(Vec3d eye, BlockHitResult expected) {
        Vec3d inside = expected.getPos().offset(expected.getSide().getOpposite(), HIT_INSET);
        BlockHitResult actual = CLIENT.world.raycast(new RaycastContext(eye, inside,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, CLIENT.player));
        return actual.getType() == HitResult.Type.BLOCK
                && actual.getBlockPos().equals(expected.getBlockPos())
                && actual.getSide() == expected.getSide();
    }

    public static Vec3d pointOnFace(Box bounds, Direction face, double u, double v) {
        Vec3d center = new Vec3d(
                face == Direction.WEST ? bounds.minX : face == Direction.EAST ? bounds.maxX : (bounds.minX + bounds.maxX) * 0.5,
                face == Direction.DOWN ? bounds.minY : face == Direction.UP ? bounds.maxY : (bounds.minY + bounds.maxY) * 0.5,
                face == Direction.NORTH ? bounds.minZ : face == Direction.SOUTH ? bounds.maxZ : (bounds.minZ + bounds.maxZ) * 0.5);
        return switch (face.getAxis()) {
            case X -> center.add(0.0, u * bounds.getLengthY(), v * bounds.getLengthZ());
            case Y -> center.add(u * bounds.getLengthX(), 0.0, v * bounds.getLengthZ());
            case Z -> center.add(u * bounds.getLengthX(), v * bounds.getLengthY(), 0.0);
            default -> throw new MatchException(null, null);
        };
    }
}
