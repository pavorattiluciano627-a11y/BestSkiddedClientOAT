package dev.echo.client.events.render;

import dev.echo.client.events.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Notification when a block state's rendering seed is requested. */
public class BlockRenderSeedEvent extends Event {
    /** The queried block state. */
    public final BlockState state;
    /** The position supplied to the query; the original reference is retained. */
    public final BlockPos position;

    /**
     * Captures the query arguments without copying or replacing them.
     * @param state queried block state
     * @param position position supplied to the query
     */
    public BlockRenderSeedEvent(BlockState state, BlockPos position) {
        this.state = state;
        this.position = position;
    }
}
