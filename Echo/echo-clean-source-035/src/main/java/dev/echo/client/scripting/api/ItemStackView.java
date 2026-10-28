package dev.echo.client.scripting.api;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

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
        return stack.getMaxCount();
    }

    public String getName() {
        return stack.getName().getString();
    }

    public String getId() {
        return Registries.ITEM.getId(stack.getItem()).toString();
    }

    public String getLore() {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (Text line : lore.lines()) {
            // The separator depends on accumulated text, not the line index.
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append(line.getString());
        }
        return text.toString();
    }
}
