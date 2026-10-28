package dev.echo.client.utils.placement;

import dev.echo.client.MinecraftAccess;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;

/** Complete target-selection source candidate with original iteration and visibility semantics. */
public final class PlacementTargets implements MinecraftAccess {
    private static final double FACE_SAMPLE_INSET = 0.49;
    private static final double[][] FACE_SAMPLES = {
            {0.0, 0.0}, {FACE_SAMPLE_INSET, FACE_SAMPLE_INSET}, {FACE_SAMPLE_INSET, -FACE_SAMPLE_INSET},
            {-FACE_SAMPLE_INSET, FACE_SAMPLE_INSET}, {-FACE_SAMPLE_INSET, -FACE_SAMPLE_INSET}
    };

    private PlacementTargets() {
    }

    public static PlacementTarget find(BlockPos position, boolean requireVisible) {
        return find(position, requireVisible, false);
    }

    public static PlacementTarget find(BlockPos position, boolean requireVisible, boolean allowPartialSupport) {
        return find(position, requireVisible, allowPartialSupport, false);
    }

    public static PlacementTarget find(BlockPos position, boolean requireVisible,
            boolean allowPartialSupport, boolean ignoreEntities) {
        if (position == null || CLIENT.world == null || CLIENT.player == null) {
            return null;
        }
        Vec3d eye = CLIENT.player.getEyePos();
        PlacementTarget nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        if (requireVisible) {
            PlacementTarget directlyHit = findReplaceableOutline(eye, position);
            if (directlyHit != null) {
                nearest = directlyHit;
                nearestDistance = eye.squaredDistanceTo(directlyHit.hitResult().getPos());
            }
        }
        for (Direction neighborDirection : Direction.values()) {
            BlockPos neighbor = position.offset(neighborDirection);
            Direction face = neighborDirection.getOpposite();
            if (!supportsFace(neighbor, face, allowPartialSupport)) {
                continue;
            }
            PlacementTarget candidate = findAgainstNeighbor(eye, position, neighbor, neighborDirection,
                    face, requireVisible, ignoreEntities);
            if (candidate == null) {
                continue;
            }
            double distance = eye.squaredDistanceTo(candidate.hitResult().getPos());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static PlacementTarget findReplaceableOutline(Vec3d eye, BlockPos position) {
        BlockState state = CLIENT.world.getBlockState(position);
        if (!state.isReplaceable()) {
            return null;
        }
        VoxelShape shape = state.getOutlineShape(CLIENT.world, position);
        if (shape.isEmpty()) {
            return null;
        }
        for (Box bounds : shape.getBoundingBoxes()) {
            Vec3d center = new Vec3d(position.getX() + (bounds.minX + bounds.maxX) * 0.5,
                    position.getY() + (bounds.minY + bounds.maxY) * 0.5,
                    position.getZ() + (bounds.minZ + bounds.maxZ) * 0.5);
            BlockHitResult hit = CLIENT.world.raycast(new RaycastContext(eye, center,
                    RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, CLIENT.player));
            if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(position)) {
                return new PlacementTarget(position, hit);
            }
        }
        return null;
    }

    public static boolean hasSupport(BlockPos position) {
        return hasSupport(position, false);
    }

    public static boolean hasSupport(BlockPos position, boolean allowPartialSupport) {
        if (position == null || CLIENT.world == null) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            if (supportsFace(position.offset(direction), direction.getOpposite(), allowPartialSupport)) {
                return true;
            }
        }
        return false;
    }

    public static boolean supportsFace(BlockPos position, Direction face, boolean allowPartialSupport) {
        BlockState state = CLIENT.world.getBlockState(position);
        if (state.isSideSolidFullSquare(CLIENT.world, position, face)) {
            return true;
        }
        return allowPartialSupport && !state.isReplaceable()
                && !state.getOutlineShape(CLIENT.world, position).isEmpty()
                && !state.getCollisionShape(CLIENT.world, position).isEmpty();
    }

    private static PlacementTarget findAgainstNeighbor(Vec3d eye, BlockPos position, BlockPos neighbor,
            Direction neighborDirection, Direction face, boolean requireVisible, boolean ignoreEntities) {
        VoxelShape outline = CLIENT.world.getBlockState(neighbor).getOutlineShape(CLIENT.world, neighbor);
        if (outline.isEmpty()) {
            return null;
        }
        Box bounds = outline.getBoundingBox().offset(neighbor);
        double reachSquared = MathHelper.square(CLIENT.player.getBlockInteractionRange());
        for (double[] offset : FACE_SAMPLES) {
            Vec3d point = PlacementRaycasts.pointOnFace(bounds, face, offset[0], offset[1]);
            BlockHitResult hit = new BlockHitResult(point, face, neighbor, false);
            if (eye.squaredDistanceTo(point) > reachSquared || !PlacementRaycasts.facesEye(hit, eye)) {
                continue;
            }
            if (requireVisible) {
                if (!PlacementRaycasts.canSeeHit(eye, hit)) {
                    continue;
                }
                if (!ignoreEntities && PlacementRaycasts.hitsEntity(eye, point)) {
                    continue;
                }
            }
            return new PlacementTarget(position, hit);
        }
        return null;
    }
}
