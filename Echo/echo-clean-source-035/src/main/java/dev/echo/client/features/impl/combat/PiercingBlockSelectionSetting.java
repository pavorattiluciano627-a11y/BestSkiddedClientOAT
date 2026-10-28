package dev.echo.client.features.impl.combat;

import dev.echo.client.setting.RegistryListSetting;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Collision-free blocks which permit Piercing's interaction-priority path. */
final class PiercingBlockSelectionSetting extends RegistryListSetting<Block> {
    PiercingBlockSelectionSetting(CharSequence label, Supplier<Registry<Block>> registry) {
        super(label, registry);
        setEntrySelected(Blocks.COBWEB, true);
    }

    @Override
    public Function<Block, String> getEntryLabel() {
        return block -> new ItemStack(block.asItem()).getHoverName().getString();
    }

    @Override
    public Predicate<Block> getEntryFilter() {
        return block -> block.defaultBlockState().getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
    }

    @Override
    public boolean isVisible() {
        return PiercingFeature.interact.getValue();
    }
}
