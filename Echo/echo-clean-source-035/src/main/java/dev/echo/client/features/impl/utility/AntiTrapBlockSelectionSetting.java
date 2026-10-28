package dev.echo.client.features.impl.utility;

import dev.echo.client.setting.RegistryListSetting;
import dev.echo.client.setting.list.RegistryEntryGroup;
import dev.echo.client.utils.blocks.BlockGroups;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Anti Trap's block selector with the shared block groups and localized item labels. */
final class AntiTrapBlockSelectionSetting extends RegistryListSetting<Block> {
    AntiTrapBlockSelectionSetting(CharSequence label, Supplier<Registry<Block>> registry) {
        super(label, registry);
    }

    @Override
    public List<RegistryEntryGroup<Block>> getGroups() {
        return BlockGroups.copyGroups();
    }

    @Override
    public Function<Block, String> getEntryLabel() {
        return block -> new ItemStack(block.asItem()).getHoverName().getString();
    }
}
