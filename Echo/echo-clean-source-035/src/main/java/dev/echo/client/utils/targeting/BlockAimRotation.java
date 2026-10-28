package dev.echo.client.utils.targeting;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.utils.placement.BlockPlacement;
import dev.echo.client.utils.placement.PlacementTarget;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Whole preferred-below-block hit provider; original candidate domains remain explicit. */
public final class BlockAimRotation implements MinecraftAccess {
    private BlockAimRotation() {
    }

    public static BlockHitResult findHit(BlockPos position, Vec3d referencePoint) {
        Vec3d eye = CLIENT.player.getCameraPosVec(1.0F);
        BlockHitResult below = new BlockHitResult(
                clampHorizontalPoint(position, referencePoint, position.getY()),
                Direction.UP, position.down(), false);
        if (BlockPlacement.facesEye(below, eye) && BlockPlacement.canSeeHit(eye, below)) {
            return below;
        }
        PlacementTarget target = BlockPlacement.find(position, true);
        return target == null ? null : target.hitResult();
    }

    public static Vec3d insetBottomPoint(BlockPos position, Vec3d referencePoint) {
        return clampHorizontalPoint(position, referencePoint, position.getY() + 0.0625);
    }

    private static Vec3d clampHorizontalPoint(BlockPos block, Vec3d point, double height) {
        return new Vec3d(
                MathHelper.clamp(point.x, block.getX() + 0.05, block.getX() + 0.95),
                height,
                MathHelper.clamp(point.z, block.getZ() + 0.05, block.getZ() + 0.95));
    }
}
