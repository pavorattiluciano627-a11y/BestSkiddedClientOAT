package dev.echo.client.utils.blocks;

import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraft.world.level.block.entity.BlockEntityTypes;
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
        return type == BlockEntityTypes.CHEST
                || type == BlockEntityTypes.BARREL
                || type == BlockEntityTypes.SHULKER_BOX
                || type == BlockEntityTypes.ENDER_CHEST;
    }

    public static Block representativeBlock(BlockEntityType<?> type) {
        if (type == BlockEntityTypes.CHEST) {
            return Blocks.CHEST;
        }
        if (type == BlockEntityTypes.BARREL) {
            return Blocks.BARREL;
        }
        if (type == BlockEntityTypes.SHULKER_BOX) {
            return Blocks.SHULKER_BOX;
        }
        if (type == BlockEntityTypes.ENDER_CHEST) {
            return Blocks.ENDER_CHEST;
        }
        return null;
    }
}
