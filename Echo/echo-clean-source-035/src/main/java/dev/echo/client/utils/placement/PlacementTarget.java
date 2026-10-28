package dev.echo.client.utils.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

/** Analysis-only record candidate pending the two original accessor targets. */
public record PlacementTarget(BlockPos position, BlockHitResult hitResult) {
}
