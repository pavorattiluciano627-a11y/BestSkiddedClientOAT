package dev.echo.client.scripting.api;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

public class ItemStackView {
    private final ItemStack stack;

    public ItemStackView(ItemStack stack) {
        this.stack = stack;
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public int getCount() {
        return stack.getCount();
    }

    public int getMaxCount() {
        return stack.getMaxStackSize();
    }

    public String getName() {
        return stack.getHoverName().getString();
    }

    public String getId() {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public String getLore() {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (Component line : lore.lines()) {
            // The separator depends on accumulated text, not the line index.
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append(line.getString());
        }
        return text.toString();
    }
}
