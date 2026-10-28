package dev.echo.client.utils.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;

/** Key-independent item-group predicates recovered from the group factory. */
public final class ItemGroupPredicates {
    private ItemGroupPredicates() {
    }

    public static boolean isInTag(Item item, TagKey<Item> tag) {
        return new ItemStack(item).is(tag);
    }

    public static boolean isMace(Item item) {
        return item instanceof MaceItem;
    }

    public static boolean hasFoodComponent(Item item) {
        return new ItemStack(item).has(DataComponents.FOOD);
    }

    public static boolean isElytra(Item item) {
        return item == Items.ELYTRA;
    }

    public static boolean isAir(Item item) {
        return item == Items.AIR;
    }
}
