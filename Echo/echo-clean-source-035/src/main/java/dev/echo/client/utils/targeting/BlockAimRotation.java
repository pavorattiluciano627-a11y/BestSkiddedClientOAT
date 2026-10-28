package dev.echo.client.utils.targeting;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.utils.placement.BlockPlacement;
import dev.echo.client.utils.placement.PlacementTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Whole preferred-below-block hit provider; original candidate domains remain explicit. */
public final class BlockAimRotation implements MinecraftAccess {
    private BlockAimRotation() {
    }

    public static BlockHitResult findHit(BlockPos position, Vec3 referencePoint) {
        Vec3 eye = CLIENT.player.getEyePosition(1.0F);
        BlockHitResult below = new BlockHitResult(
                clampHorizontalPoint(position, referencePoint, position.getY()),
                Direction.UP, position.below(), false);
        if (BlockPlacement.facesEye(below, eye) && BlockPlacement.canSeeHit(eye, below)) {
            return below;
        }
        PlacementTarget target = BlockPlacement.find(position, true);
        return target == null ? null : target.hitResult();
    }

    public static Vec3 insetBottomPoint(BlockPos position, Vec3 referencePoint) {
        return clampHorizontalPoint(position, referencePoint, position.getY() + 0.0625);
    }

    private static Vec3 clampHorizontalPoint(BlockPos block, Vec3 point, double height) {
        return new Vec3(
                Mth.clamp(point.x, block.getX() + 0.05, block.getX() + 0.95),
                height,
                Mth.clamp(point.z, block.getZ() + 0.05, block.getZ() + 0.95));
    }
}
