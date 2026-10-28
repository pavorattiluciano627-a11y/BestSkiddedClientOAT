package dev.echo.client.utils.placement;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/** Candidate placement position and its accepted stack predicate. */
public record PlacementFilter(BlockPos position, Predicate<ItemStack> acceptsStack) {
}
