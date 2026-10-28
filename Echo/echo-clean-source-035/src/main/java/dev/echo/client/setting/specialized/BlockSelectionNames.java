package dev.echo.client.setting.specialized;

import java.util.function.Function;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

/** The item-stack display-name provider used by Search and Scaffold. */
public final class BlockSelectionNames {
    private BlockSelectionNames() {
    }

    public static Function<Block, String> nameProvider() {
        return BlockSelectionNames::displayName;
    }

    public static String displayName(Block block) {
        return new ItemStack(block.asItem()).getName().getString();
    }
}
