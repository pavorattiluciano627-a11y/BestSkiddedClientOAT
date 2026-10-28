package dev.echo.client.utils.blocks;

import net.minecraft.block.AbstractChestBlock;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CrafterBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntityType;

/** Analysis-only reconstruction pending the original guard and direct-target proofs. */
public class ContainerBlocks {
    public static boolean isContainer(Block block) {
        return block instanceof AbstractChestBlock<?>
                || block instanceof BarrelBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof HopperBlock
                || block instanceof DispenserBlock
                || block instanceof CrafterBlock
                || block instanceof AbstractFurnaceBlock;
    }

    public static boolean hasRepresentativeBlock(BlockEntityType<?> type) {
        return type == BlockEntityType.CHEST
                || type == BlockEntityType.BARREL
                || type == BlockEntityType.SHULKER_BOX
                || type == BlockEntityType.ENDER_CHEST;
    }

    public static Block representativeBlock(BlockEntityType<?> type) {
        if (type == BlockEntityType.CHEST) {
            return Blocks.CHEST;
        }
        if (type == BlockEntityType.BARREL) {
            return Blocks.BARREL;
        }
        if (type == BlockEntityType.SHULKER_BOX) {
            return Blocks.SHULKER_BOX;
        }
        if (type == BlockEntityType.ENDER_CHEST) {
            return Blocks.ENDER_CHEST;
        }
        return null;
    }
}
