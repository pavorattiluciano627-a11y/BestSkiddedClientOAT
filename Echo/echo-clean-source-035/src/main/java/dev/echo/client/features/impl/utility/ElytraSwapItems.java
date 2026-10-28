package dev.echo.client.features.impl.utility;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Exact item-identity predicate used by Elytra Swap's target-slot search. */
public final class ElytraSwapItems {
    private ElytraSwapItems() {}

    public static boolean isElytra(ItemStack stack) {
        return stack.getItem() == Items.ELYTRA;
    }
}
