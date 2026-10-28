package dev.echo.client.setting;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.setting.list.RegistryEntryGroup;
import dev.echo.client.utils.items.ItemGroups;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Item registry selection, with the original empty-selection main-hand policy. */
public class ItemListSetting extends RegistryListSetting<Item> implements MinecraftAccess {
    public ItemListSetting(CharSequence label) {
        super(label, () -> BuiltInRegistries.ITEM);
    }

    public ItemListSetting(CharSequence label, Predicate<Object> visibleWhen) {
        super(label, () -> BuiltInRegistries.ITEM, visibleWhen);
    }

    @Override
    public String getTypeId() {
        return "itm";
    }

    @Override
    public List<RegistryEntryGroup<Item>> getGroups() {
        return ItemGroups.copyGroups();
    }

    @Override
    public Function<Item, String> getEntryLabel() {
        return item -> new ItemStack(item).getHoverName().getString();
    }

    public boolean acceptsHeldItem() {
        if (getSelectedCount() == 0) {
            return true;
        }
        if (CLIENT.player == null) {
            return false;
        }
        ItemStack stack = CLIENT.player.getMainHandItem();
        return isEntrySelected(stack.isEmpty() ? Items.AIR : stack.getItem());
    }
}
