package dev.echo.client.features.impl.utility;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.interaction.ItemUseEvent;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.mixin.MinecraftClientAccessor;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.ItemListSetting;
import dev.echo.client.setting.specialized.FastUseItemListSetting;
import net.minecraft.item.Item;

/** Reduces the client item-use cooldown after an allowed item-use operation. */
public class FastUseFeature extends Feature {
    public static final CharSequence BLOCKS_GROUP = CatalogText.literal("Blocks");
    public static final CharSequence FOOD_GROUP = CatalogText.literal("Food");
    public static final CharSequence POTIONS_GROUP = CatalogText.literal("Potions");
    public static final CharSequence ENDER_PEARLS_GROUP = CatalogText.literal("Ender Pearls");
    public static final CharSequence THROWABLES_GROUP = CatalogText.literal("Throwables");
    public static final CharSequence BUCKETS_GROUP = CatalogText.literal("Buckets");
    public static final CharSequence FIREWORKS_GROUP = CatalogText.literal("Fireworks");
    public static FloatRangeSetting delay;
    public static ItemListSetting items = new FastUseItemListSetting(CatalogText.literal("Items"));

    public FastUseFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Fast Use"), dev.echo.client.bootstrap.CatalogText.literal("Faster item use"), dev.echo.client.features.Category.UTILITY));
    }

    @Override
    public void postInitialize() {
        delay.tooltip(CatalogText.literal("Timing"));
    }

    @Subscribe
    private void onItemUsePost(ItemUseEvent.Post event) {
        if (isOutsideWorld()) {
            return;
        }
        if (client.interactionManager == null || client.interactionManager.isBreakingBlock()) {
            return;
        }
        if (items.getSelectedCount() > 0) {
            Item mainHandItem = client.player.getMainHandStack().getItem();
            Item offHandItem = client.player.getOffHandStack().getItem();
            if (!items.isEntrySelected(mainHandItem) && !items.isEntrySelected(offHandItem)) {
                return;
            }
        }
        ((MinecraftClientAccessor) client).setItemUseCooldown((int) delay.sampleUniform());
    }

}
