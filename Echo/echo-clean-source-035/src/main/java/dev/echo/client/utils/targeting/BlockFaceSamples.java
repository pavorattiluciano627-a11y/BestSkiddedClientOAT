package dev.echo.client.utils.targeting;

import dev.echo.client.MinecraftAccess;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Whole block-face sampler; original key, marker and cache domains remain explicit. */
public final class BlockFaceSamples implements MinecraftAccess {
    private static int[] samplingMarker;

    static {
        if (getSamplingMarker() == null) {
            setSamplingMarker(new int[4]);
        }
    }

    private BlockFaceSamples() {
    }

    public static Vec3 findClearAimPoint(BlockPos position, float partialTick) {
        if (position == null) {
            return null;
        }
        if (CLIENT.player == null || CLIENT.level == null) {
            return Vec3.atCenterOf(position);
        }
        // The original always supplies 1.0F here; partialTick is not read.
        Vec3 eye = CLIENT.player.getEyePosition(1.0F);
        Vec3 center = Vec3.atCenterOf(position);
        Vec3 fromCenter = eye.subtract(center);
        Direction[] faces = Direction.values().clone();
        Arrays.sort(faces, (first, second) -> compareFacing(fromCenter, first, second));
        for (Direction face : faces) {
            if (directionDot(face, fromCenter) <= 0.0) {
                continue;
            }
            Vec3 clearPoint = findOnFace(eye, position, center, face);
            if (clearPoint != null) {
                return clearPoint;
            }
        }
        return center;
    }

    private static int compareFacing(Vec3 fromCenter, Direction first, Direction second) {
        return Double.compare(directionDot(second, fromCenter), directionDot(first, fromCenter));
    }

    private static double directionDot(Direction direction, Vec3 displacement) {
        // Multiplication is intentional: a zero component multiplied by infinity is NaN.
        return direction.getStepX() * displacement.x
                + direction.getStepY() * displacement.y
                + direction.getStepZ() * displacement.z;
    }

    public static boolean rayHitsBlock(Vec3 start, Vec3 point, BlockPos expectedBlock) {
        if (CLIENT.level == null || CLIENT.player == null) {
            return false;
        }
        Vec3 direction = point.subtract(start);
        if (direction.lengthSqr() < 1.0E-8) {
            return true;
        }
        Vec3 end = start.add(direction.normalize().scale(CLIENT.player.blockInteractionRange()));
        BlockHitResult hit = CLIENT.level.clip(new ClipContext(start, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CLIENT.player));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(expectedBlock);
    }

    private static Vec3 findOnFace(Vec3 eye, BlockPos position, Vec3 center, Direction face) {
        double faceX = center.x + face.getStepX() * 0.5;
        double faceY = center.y + face.getStepY() * 0.5;
        double faceZ = center.z + face.getStepZ() * 0.5;
        Direction.Axis axis = face.getAxis();
        double towardEyeU;
        double towardEyeV;
        switch (axis) {
            case X -> {
                towardEyeU = eye.y >= center.y ? 1.0 : -1.0;
                towardEyeV = eye.z >= center.z ? 1.0 : -1.0;
            }
            case Y -> {
                towardEyeU = eye.x >= center.x ? 1.0 : -1.0;
                towardEyeV = eye.z >= center.z ? 1.0 : -1.0;
            }
            case Z -> {
                towardEyeU = eye.x >= center.x ? 1.0 : -1.0;
                towardEyeV = eye.y >= center.y ? 1.0 : -1.0;
            }
            default -> throw new MatchException(null, null);
        }
        double randomInset = 0.3 + ThreadLocalRandom.current().nextDouble() * 0.1;
        double[][] offsets = {
                {towardEyeU * randomInset, towardEyeV * randomInset},
                {towardEyeU * 0.25, towardEyeV * 0.25},
                {towardEyeU * 0.2, towardEyeV * 0.2},
                {towardEyeU * 0.15, towardEyeV * 0.15},
                {towardEyeU * 0.1, towardEyeV * 0.1},
                {towardEyeU * 0.3, 0.0},
                {0.0, towardEyeV * 0.3},
                {0.0, 0.0},
                {-towardEyeU * 0.3, towardEyeV * 0.3},
                {towardEyeU * 0.3, -towardEyeV * 0.3},
                {-towardEyeU * 0.3, -towardEyeV * 0.3},
                {-towardEyeU * 0.3, 0.0},
                {0.0, -towardEyeV * 0.3}
        };
        for (double[] offset : offsets) {
            Vec3 point = offsetOnFace(axis, faceX, faceY, faceZ, offset[0], offset[1]);
            if (rayHitsBlock(eye, point, position)) {
                return point;
            }
        }
        return null;
    }

    private static Vec3 offsetOnFace(Direction.Axis axis, double x, double y, double z, double u, double v) {
        return switch (axis) {
            case X -> new Vec3(x, y + u, z + v);
            case Y -> new Vec3(x + u, y, z + v);
            case Z -> new Vec3(x + u, y + v, z);
            default -> throw new MatchException(null, null);
        };
    }

    public static void setSamplingMarker(int[] marker) {
        samplingMarker = marker;
    }

    public static int[] getSamplingMarker() {
        return samplingMarker;
    }
}
