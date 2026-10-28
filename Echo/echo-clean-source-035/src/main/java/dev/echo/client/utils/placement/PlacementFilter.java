package dev.echo.client.utils.placement;

import java.util.function.Predicate;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/** Candidate placement position and its accepted stack predicate. */
public record PlacementFilter(BlockPos position, Predicate<ItemStack> acceptsStack) {
}
