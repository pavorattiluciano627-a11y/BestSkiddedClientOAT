package dev.echo.client.features.impl.combat;

import dev.echo.client.setting.RegistryListSetting;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockView;

/** Collision-free blocks which permit Piercing's interaction-priority path. */
final class PiercingBlockSelectionSetting extends RegistryListSetting<Block> {
    PiercingBlockSelectionSetting(CharSequence label, Supplier<Registry<Block>> registry) {
        super(label, registry);
        setEntrySelected(Blocks.COBWEB, true);
    }

    @Override
    public Function<Block, String> getEntryLabel() {
        return block -> new ItemStack(block.asItem()).getName().getString();
    }

    @Override
    public Predicate<Block> getEntryFilter() {
        return block -> block.getDefaultState().getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).isEmpty();
    }

    @Override
    public boolean isVisible() {
        return PiercingFeature.interact.getValue();
    }
}
