package dev.echo.client.utils.items;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.ItemTags;

/** Item queries whose original bodies contain no application key or custom dispatch. */
public final class ItemProperties {
    private ItemProperties() {
    }

    public static boolean isFood(ItemStack stack) {
        return stack.getComponents().contains(DataComponentTypes.FOOD);
    }

    public static boolean isAxe(ItemStack stack) {
        return stack.isIn(ItemTags.AXES);
    }

    public static boolean isMace(ItemStack stack) {
        return stack.getItem() instanceof MaceItem;
    }

    public static boolean isGlowstone(ItemStack stack) {
        return stack.isOf(Items.GLOWSTONE);
    }

    public static boolean hasEnchantment(ItemStack stack, RegistryKey<Enchantment> enchantment) {
        return stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
                .getEnchantments().stream().anyMatch(entry -> entry.matchesKey(enchantment));
    }
}
