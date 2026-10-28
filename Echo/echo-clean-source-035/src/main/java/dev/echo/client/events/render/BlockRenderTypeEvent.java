package dev.echo.client.events.render;

import dev.echo.client.events.Event;
import net.minecraft.block.Block;

/** Block render-type query whose cancellation requests invisible rendering. */
public class BlockRenderTypeEvent extends Event {
    /** The block whose render type is being queried. */
    public final Block block;

    /**
     * Captures the queried block.
     * @param block block whose render type is being queried
     */
    public BlockRenderTypeEvent(Block block) {
        this.block = block;
    }
}
