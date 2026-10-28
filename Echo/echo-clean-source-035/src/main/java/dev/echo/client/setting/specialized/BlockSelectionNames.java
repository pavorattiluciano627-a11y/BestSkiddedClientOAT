package dev.echo.client.setting.specialized;

import java.util.function.Function;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** The item-stack display-name provider used by Search and Scaffold. */
public final class BlockSelectionNames {
    private BlockSelectionNames() {
    }

    public static Function<Block, String> nameProvider() {
        return BlockSelectionNames::displayName;
    }

    public static String displayName(Block block) {
        return new ItemStack(block.asItem()).getHoverName().getString();
    }
}
