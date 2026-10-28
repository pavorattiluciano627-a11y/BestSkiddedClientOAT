package dev.echo.client.utils.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;

/** Block-state shape checks that do not require a loaded client world. */
public final class BlockShapeChecks {
    private BlockShapeChecks() {
    }

    public static boolean isDefaultStateFullCube(Block block) {
        return block.defaultBlockState().isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }
}
