package dev.echo.client.setting;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.setting.list.RegistryEntryGroup;
import dev.echo.client.utils.items.ItemGroups;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

/** Item registry selection, with the original empty-selection main-hand policy. */
public class ItemListSetting extends RegistryListSetting<Item> implements MinecraftAccess {
    public ItemListSetting(CharSequence label) {
        super(label, () -> Registries.ITEM);
    }

    public ItemListSetting(CharSequence label, Predicate<Object> visibleWhen) {
        super(label, () -> Registries.ITEM, visibleWhen);
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
        return item -> new ItemStack(item).getName().getString();
    }

    public boolean acceptsHeldItem() {
        if (getSelectedCount() == 0) {
            return true;
        }
        if (CLIENT.player == null) {
            return false;
        }
        ItemStack stack = CLIENT.player.getMainHandStack();
        return isEntrySelected(stack.isEmpty() ? Items.AIR : stack.getItem());
    }
}
