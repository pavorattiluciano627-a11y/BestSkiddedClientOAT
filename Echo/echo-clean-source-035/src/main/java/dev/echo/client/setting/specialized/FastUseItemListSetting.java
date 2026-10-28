package dev.echo.client.setting.specialized;

import dev.echo.client.features.impl.utility.FastUseFeature;
import dev.echo.client.setting.ItemListSetting;
import dev.echo.client.setting.list.RegistryEntryGroup;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;

/** The seven original Fast Use entry groups, in their original display order. */
public class FastUseItemListSetting extends ItemListSetting {
    public FastUseItemListSetting(CharSequence label) {
        super(label);
    }

    @Override
    public List<RegistryEntryGroup<Item>> getGroups() {
        // The records/list are fresh on each invocation; the labels are shared.
        return List.of(
                new RegistryEntryGroup<>(FastUseFeature.BLOCKS_GROUP, FastUseItemListSetting::isBlock),
                new RegistryEntryGroup<>(FastUseFeature.FOOD_GROUP, FastUseItemListSetting::isFood),
                new RegistryEntryGroup<>(FastUseFeature.POTIONS_GROUP, FastUseItemListSetting::isPotion),
                new RegistryEntryGroup<>(FastUseFeature.ENDER_PEARLS_GROUP, FastUseItemListSetting::isEnderPearl),
                new RegistryEntryGroup<>(FastUseFeature.THROWABLES_GROUP, FastUseItemListSetting::isThrowable),
                new RegistryEntryGroup<>(FastUseFeature.BUCKETS_GROUP, FastUseItemListSetting::isFilledBucket),
                new RegistryEntryGroup<>(FastUseFeature.FIREWORKS_GROUP, FastUseItemListSetting::isFirework));
    }

    private static boolean isBlock(Item item) {
        return item instanceof BlockItem;
    }

    private static boolean isFood(Item item) {
        return new ItemStack(item).has(DataComponents.FOOD);
    }

    private static boolean isPotion(Item item) {
        return item instanceof PotionItem;
    }

    private static boolean isEnderPearl(Item item) {
        return item instanceof EnderpearlItem;
    }

    private static boolean isThrowable(Item item) {
        return item == Items.SNOWBALL || item == Items.EGG
                || item == Items.WIND_CHARGE || item == Items.EXPERIENCE_BOTTLE;
    }

    private static boolean isFilledBucket(Item item) {
        return item == Items.WATER_BUCKET || item == Items.LAVA_BUCKET
                || item == Items.MILK_BUCKET || item == Items.POWDER_SNOW_BUCKET;
    }

    private static boolean isFirework(Item item) {
        return item == Items.FIREWORK_ROCKET;
    }
}
