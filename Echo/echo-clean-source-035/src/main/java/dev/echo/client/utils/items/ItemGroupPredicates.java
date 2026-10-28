package dev.echo.client.utils.items;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.registry.tag.TagKey;

/** Key-independent item-group predicates recovered from the group factory. */
public final class ItemGroupPredicates {
    private ItemGroupPredicates() {
    }

    public static boolean isInTag(Item item, TagKey<Item> tag) {
        return new ItemStack(item).isIn(tag);
    }

    public static boolean isMace(Item item) {
        return item instanceof MaceItem;
    }

    public static boolean hasFoodComponent(Item item) {
        return new ItemStack(item).contains(DataComponentTypes.FOOD);
    }

    public static boolean isElytra(Item item) {
        return item == Items.ELYTRA;
    }

    public static boolean isAir(Item item) {
        return item == Items.AIR;
    }
}
