package dev.echo.client.setting.specialized;

import dev.echo.client.setting.RegistryListSetting;
import dev.echo.client.setting.list.RegistryEntryGroup;
import dev.echo.client.utils.blocks.BlockGroups;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.registry.Registry;

/** Search block selections with the shared block groups and item-stack display names. */
public class SearchBlockListSetting extends RegistryListSetting<Block> {
    public SearchBlockListSetting(CharSequence label, Supplier<Registry<Block>> registry) {
        super(label, registry);
    }

    @Override
    public List<RegistryEntryGroup<Block>> getGroups() {
        return BlockGroups.copyGroups();
    }

    @Override
    public Function<Block, String> getEntryLabel() {
        return BlockSelectionNames.nameProvider();
    }
}
