package dev.echo.client.utils.placement;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

/** Analysis-only record candidate pending the two original accessor targets. */
public record PlacementTarget(BlockPos position, BlockHitResult hitResult) {
}
