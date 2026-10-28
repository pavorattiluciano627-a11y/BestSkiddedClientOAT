package dev.echo.client.utils.placement;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockView;

/** Block-state shape checks that do not require a loaded client world. */
public final class BlockShapeChecks {
    private BlockShapeChecks() {
    }

    public static boolean isDefaultStateFullCube(Block block) {
        return block.getDefaultState().isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
    }
}
