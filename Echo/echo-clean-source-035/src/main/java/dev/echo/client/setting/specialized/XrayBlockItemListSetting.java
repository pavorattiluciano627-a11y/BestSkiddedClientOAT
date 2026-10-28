package dev.echo.client.setting.specialized;

import dev.echo.client.features.impl.render.XrayFeature;
import dev.echo.client.setting.ItemListSetting;
import dev.echo.client.setting.list.RegistryEntryGroup;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/** Xray's block-item filter and initial selection. */
public class XrayBlockItemListSetting extends ItemListSetting {
    public XrayBlockItemListSetting(CharSequence label) {
        super(label);
        for (Item item : XrayFeature.getDefaultItems()) {
            setEntrySelected(item, true);
        }
    }

    @Override
    public Predicate<Item> getEntryFilter() {
        return item -> item instanceof BlockItem;
    }

    @Override
    public List<RegistryEntryGroup<Item>> getGroups() {
        return XrayFeature.getOreGroups();
    }
}
