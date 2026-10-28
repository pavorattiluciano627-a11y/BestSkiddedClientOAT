package dev.echo.client.utils.items;

import dev.echo.client.setting.list.RegistryEntryGroup;
import dev.echo.client.text.SegmentedText;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;

/** Lazily initialized, shared item-group objects with fresh outer-list copies. */
public final class ItemGroups {
    private ItemGroups() {
    }

    private static RegistryEntryGroup<Item> createTagGroup(CharSequence label, TagKey<Item> tag) {
        return new RegistryEntryGroup<>(label, item -> new ItemStack(item).is(tag));
    }

    private static List<RegistryEntryGroup<Item>> createGroups() {
        List<RegistryEntryGroup<Item>> groups = new ArrayList<>();
        groups.add(createTagGroup(SegmentedText.literal("Swords"), ItemTags.SWORDS));
        groups.add(createTagGroup(SegmentedText.literal("Axes"), ItemTags.AXES));
        groups.add(new RegistryEntryGroup<>(SegmentedText.literal("Mace"), item -> item instanceof MaceItem));
        groups.add(new RegistryEntryGroup<>(SegmentedText.literal("Elytra"), item -> item == Items.ELYTRA));
        groups.add(new RegistryEntryGroup<>(SegmentedText.literal("Fists"), item -> item == Items.AIR));
        groups.add(createTagGroup(SegmentedText.literal("Helmet"), ItemTags.HEAD_ARMOR));
        groups.add(createTagGroup(SegmentedText.literal("Chestplate"), ItemTags.CHEST_ARMOR));
        groups.add(createTagGroup(SegmentedText.literal("Leggings"), ItemTags.LEG_ARMOR));
        groups.add(createTagGroup(SegmentedText.literal("Boots"), ItemTags.FOOT_ARMOR));
        groups.add(createTagGroup(SegmentedText.literal("Pickaxes"), ItemTags.PICKAXES));
        groups.add(createTagGroup(SegmentedText.literal("Shovels"), ItemTags.SHOVELS));
        groups.add(createTagGroup(SegmentedText.literal("Hoes"), ItemTags.HOES));
        groups.add(createTagGroup(SegmentedText.literal("Spears"), ItemTags.SPEARS));
        groups.add(createTagGroup(SegmentedText.literal("Planks"), ItemTags.PLANKS));
        groups.add(new RegistryEntryGroup<>(SegmentedText.literal("Food"),
                item -> new ItemStack(item).has(DataComponents.FOOD)));
        return groups;
    }

    public static List<RegistryEntryGroup<Item>> copyGroups() {
        return new ArrayList<>(Catalog.GROUPS);
    }

    public static RegistryEntryGroup<Item> swords() {
        return Catalog.GROUPS.get(0);
    }

    public static RegistryEntryGroup<Item> axes() {
        return Catalog.GROUPS.get(1);
    }

    public static RegistryEntryGroup<Item> mace() {
        return Catalog.GROUPS.get(2);
    }

    public static RegistryEntryGroup<Item> fists() {
        return Catalog.GROUPS.get(4);
    }

    private static final class Catalog {
        private static final List<RegistryEntryGroup<Item>> GROUPS = createGroups();

        private Catalog() {
        }
    }
}
