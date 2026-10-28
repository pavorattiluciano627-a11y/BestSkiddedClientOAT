package dev.echo.client.features.impl.utility;

import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.RegistryListSetting;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockView;

/** Full-block selection for the corresponding Auto Trap mode. */
public final class AutoTrapBlockSelectionSetting extends RegistryListSetting<Block> {
    private final Supplier<ChoiceSetting> mode;
    private final Supplier<CharSequence> fullBlockMode;

    /**
     * Creates the selector with live references to the feature's static mode fields.
     * @param label setting label
     * @param registry block registry supplier
     * @param mode current mode-setting object
     * @param fullBlockMode the original sharedPacket mode value
     */
    public AutoTrapBlockSelectionSetting(CharSequence label, Supplier<Registry<Block>> registry,
                                         Supplier<ChoiceSetting> mode, Supplier<CharSequence> fullBlockMode) {
        super(label, registry);
        this.mode = mode;
        this.fullBlockMode = fullBlockMode;
        setEntrySelected(Blocks.OBSIDIAN, true);
    }

    @Override
    public Function<Block, String> getEntryLabel() {
        return block -> new ItemStack(block.asItem()).getName().getString();
    }

    @Override
    public Predicate<Block> getEntryFilter() {
        return block -> block.asItem() instanceof BlockItem
                && block.getDefaultState().isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
    }

    @Override
    public boolean isVisible() {
        return mode.get().isSelected(fullBlockMode.get());
    }
}
