package dev.echo.client.utils.targeting;

import dev.echo.client.MinecraftAccess;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

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

    public static Vec3d findClearAimPoint(BlockPos position, float partialTick) {
        if (position == null) {
            return null;
        }
        if (CLIENT.player == null || CLIENT.world == null) {
            return Vec3d.ofCenter(position);
        }
        // The original always supplies 1.0F here; partialTick is not read.
        Vec3d eye = CLIENT.player.getCameraPosVec(1.0F);
        Vec3d center = Vec3d.ofCenter(position);
        Vec3d fromCenter = eye.subtract(center);
        Direction[] faces = Direction.values().clone();
        Arrays.sort(faces, (first, second) -> compareFacing(fromCenter, first, second));
        for (Direction face : faces) {
            if (directionDot(face, fromCenter) <= 0.0) {
                continue;
            }
            Vec3d clearPoint = findOnFace(eye, position, center, face);
            if (clearPoint != null) {
                return clearPoint;
            }
        }
        return center;
    }

    private static int compareFacing(Vec3d fromCenter, Direction first, Direction second) {
        return Double.compare(directionDot(second, fromCenter), directionDot(first, fromCenter));
    }

    private static double directionDot(Direction direction, Vec3d displacement) {
        // Multiplication is intentional: a zero component multiplied by infinity is NaN.
        return direction.getOffsetX() * displacement.x
                + direction.getOffsetY() * displacement.y
                + direction.getOffsetZ() * displacement.z;
    }

    public static boolean rayHitsBlock(Vec3d start, Vec3d point, BlockPos expectedBlock) {
        if (CLIENT.world == null || CLIENT.player == null) {
            return false;
        }
        Vec3d direction = point.subtract(start);
        if (direction.lengthSquared() < 1.0E-8) {
            return true;
        }
        Vec3d end = start.add(direction.normalize().multiply(CLIENT.player.getBlockInteractionRange()));
        BlockHitResult hit = CLIENT.world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, CLIENT.player));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(expectedBlock);
    }

    private static Vec3d findOnFace(Vec3d eye, BlockPos position, Vec3d center, Direction face) {
        double faceX = center.x + face.getOffsetX() * 0.5;
        double faceY = center.y + face.getOffsetY() * 0.5;
        double faceZ = center.z + face.getOffsetZ() * 0.5;
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
            Vec3d point = offsetOnFace(axis, faceX, faceY, faceZ, offset[0], offset[1]);
            if (rayHitsBlock(eye, point, position)) {
                return point;
            }
        }
        return null;
    }

    private static Vec3d offsetOnFace(Direction.Axis axis, double x, double y, double z, double u, double v) {
        return switch (axis) {
            case X -> new Vec3d(x, y + u, z + v);
            case Y -> new Vec3d(x + u, y, z + v);
            case Z -> new Vec3d(x + u, y + v, z);
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
