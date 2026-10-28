package dev.echo.client.utils.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Item queries whose original bodies contain no application key or custom dispatch. */
public final class ItemProperties {
    private ItemProperties() {
    }

    public static boolean isFood(ItemStack stack) {
        return stack.getComponents().has(DataComponents.FOOD);
    }

    public static boolean isAxe(ItemStack stack) {
        return stack.is(ItemTags.AXES);
    }

    public static boolean isMace(ItemStack stack) {
        return stack.getItem() instanceof MaceItem;
    }

    public static boolean isGlowstone(ItemStack stack) {
        return stack.is(Items.GLOWSTONE);
    }

    public static boolean hasEnchantment(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                .keySet().stream().anyMatch(entry -> entry.is(enchantment));
    }
}
