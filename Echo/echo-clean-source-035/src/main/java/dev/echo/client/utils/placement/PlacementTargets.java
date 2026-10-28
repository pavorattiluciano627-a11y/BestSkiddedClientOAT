package dev.echo.client.utils.placement;

import dev.echo.client.MinecraftAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

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
        if (position == null || CLIENT.level == null || CLIENT.player == null) {
            return null;
        }
        Vec3 eye = CLIENT.player.getEyePosition();
        PlacementTarget nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        if (requireVisible) {
            PlacementTarget directlyHit = findReplaceableOutline(eye, position);
            if (directlyHit != null) {
                nearest = directlyHit;
                nearestDistance = eye.distanceToSqr(directlyHit.hitResult().getLocation());
            }
        }
        for (Direction neighborDirection : Direction.values()) {
            BlockPos neighbor = position.relative(neighborDirection);
            Direction face = neighborDirection.getOpposite();
            if (!supportsFace(neighbor, face, allowPartialSupport)) {
                continue;
            }
            PlacementTarget candidate = findAgainstNeighbor(eye, position, neighbor, neighborDirection,
                    face, requireVisible, ignoreEntities);
            if (candidate == null) {
                continue;
            }
            double distance = eye.distanceToSqr(candidate.hitResult().getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static PlacementTarget findReplaceableOutline(Vec3 eye, BlockPos position) {
        BlockState state = CLIENT.level.getBlockState(position);
        if (!state.canBeReplaced()) {
            return null;
        }
        VoxelShape shape = state.getShape(CLIENT.level, position);
        if (shape.isEmpty()) {
            return null;
        }
        for (AABB bounds : shape.toAabbs()) {
            Vec3 center = new Vec3(position.getX() + (bounds.minX + bounds.maxX) * 0.5,
                    position.getY() + (bounds.minY + bounds.maxY) * 0.5,
                    position.getZ() + (bounds.minZ + bounds.maxZ) * 0.5);
            BlockHitResult hit = CLIENT.level.clip(new ClipContext(eye, center,
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CLIENT.player));
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
        if (position == null || CLIENT.level == null) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            if (supportsFace(position.relative(direction), direction.getOpposite(), allowPartialSupport)) {
                return true;
            }
        }
        return false;
    }

    public static boolean supportsFace(BlockPos position, Direction face, boolean allowPartialSupport) {
        BlockState state = CLIENT.level.getBlockState(position);
        if (state.isFaceSturdy(CLIENT.level, position, face)) {
            return true;
        }
        return allowPartialSupport && !state.canBeReplaced()
                && !state.getShape(CLIENT.level, position).isEmpty()
                && !state.getCollisionShape(CLIENT.level, position).isEmpty();
    }

    private static PlacementTarget findAgainstNeighbor(Vec3 eye, BlockPos position, BlockPos neighbor,
            Direction neighborDirection, Direction face, boolean requireVisible, boolean ignoreEntities) {
        VoxelShape outline = CLIENT.level.getBlockState(neighbor).getShape(CLIENT.level, neighbor);
        if (outline.isEmpty()) {
            return null;
        }
        AABB bounds = outline.bounds().move(neighbor);
        double reachSquared = Mth.square(CLIENT.player.blockInteractionRange());
        for (double[] offset : FACE_SAMPLES) {
            Vec3 point = PlacementRaycasts.pointOnFace(bounds, face, offset[0], offset[1]);
            BlockHitResult hit = new BlockHitResult(point, face, neighbor, false);
            if (eye.distanceToSqr(point) > reachSquared || !PlacementRaycasts.facesEye(hit, eye)) {
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
