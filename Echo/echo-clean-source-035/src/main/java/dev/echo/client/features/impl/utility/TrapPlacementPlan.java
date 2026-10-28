package dev.echo.client.features.impl.utility;

import dev.echo.client.utils.placement.PlacementTarget;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

/**
 * A trap placement together with its inventory and ray-validation requirements.
 * @param target block position and placement hit
 * @param itemFilter matching inventory stacks
 * @param hotbarSlot selected hotbar slot
 * @param ignoreInterveningBlocks whether validation uses the target-block ray query
 */
public record TrapPlacementPlan(PlacementTarget target, Predicate<ItemStack> itemFilter,
                                int hotbarSlot, boolean ignoreInterveningBlocks) {
}
