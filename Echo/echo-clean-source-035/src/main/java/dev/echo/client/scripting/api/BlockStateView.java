package dev.echo.client.scripting.api;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

public class BlockStateView {
    private final BlockState state;

    public BlockStateView(BlockState state) {
        this.state = state;
    }

    public boolean isAir() {
        return state.isAir();
    }

    public String getId() {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }
}
