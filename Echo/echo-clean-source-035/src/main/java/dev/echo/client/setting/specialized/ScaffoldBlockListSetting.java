package dev.echo.client.setting.specialized;

import dev.echo.client.features.impl.world.ScaffoldFeature;
import dev.echo.client.setting.RegistryListSetting;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Scaffold's placeable-item and full supporting-face filter. */
public class ScaffoldBlockListSetting extends RegistryListSetting<Block> {
    public ScaffoldBlockListSetting(CharSequence label, Supplier<Registry<Block>> registry) {
        super(label, registry);
    }

    @Override public Function<Block, String> getEntryLabel() {
        return block -> new ItemStack(block.asItem()).getHoverName().getString();
    }

    @Override public Predicate<Block> getEntryFilter() {
        // Each original factory call allocated a predicate with captured key pieces.
        return new Predicate<>() {
            @Override public boolean test(Block block) {
                return block.asItem() instanceof BlockItem && ScaffoldFeature.isSuitableBlock(block);
            }
        };
    }
}
