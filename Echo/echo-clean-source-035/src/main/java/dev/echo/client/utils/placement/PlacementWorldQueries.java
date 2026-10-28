package dev.echo.client.utils.placement;

import dev.echo.client.MinecraftAccess;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * Complete source unit for the recovered placement queries and main-hand interaction.
 * Original dispatch and guard applicability are still tracked as finite obligations.
 */
public final class PlacementWorldQueries implements MinecraftAccess {
    private PlacementWorldQueries() {
    }

    public static boolean isBlock(BlockPos position, Block block) {
        return CLIENT.world.getBlockState(position).getBlock() == block;
    }

    public static boolean isChargedRespawnAnchor(BlockPos position) {
        return isBlock(position, Blocks.RESPAWN_ANCHOR)
                && CLIENT.world.getBlockState(position).get(RespawnAnchorBlock.CHARGES) != 0;
    }

    public static boolean isUnchargedRespawnAnchor(BlockPos position) {
        return isBlock(position, Blocks.RESPAWN_ANCHOR)
                && CLIENT.world.getBlockState(position).get(RespawnAnchorBlock.CHARGES) == 0;
    }

    public static boolean isEndCrystalBase(BlockState state) {
        return state != null && (state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK));
    }

    public static BlockPos placementPosition(BlockHitResult hit) {
        return new ItemPlacementContext(CLIENT.player, Hand.MAIN_HAND, CLIENT.player.getMainHandStack(), hit).getBlockPos();
    }

    public static ActionResult interactMainHand(BlockHitResult hit, boolean swing) {
        if (CLIENT.player == null || CLIENT.interactionManager == null) {
            return ActionResult.PASS;
        }
        ActionResult result = CLIENT.interactionManager.interactBlock(CLIENT.player, Hand.MAIN_HAND, hit);
        if (result.isAccepted() && swing && result instanceof ActionResult.Success success) {
            ActionResult.SwingSource source = success.swingSource();
            if (source == ActionResult.SwingSource.CLIENT || source == ActionResult.SwingSource.SERVER) {
                CLIENT.player.swingHand(Hand.MAIN_HAND);
            }
        }
        return result;
    }

    public static boolean hasOtherLivingEntity(BlockPos position, BlockState state) {
        if (CLIENT.world == null || position == null || state == null) {
            return false;
        }
        // The supplied state is only null-checked in the original method.
        return !CLIENT.world.getEntitiesByClass(LivingEntity.class, new Box(position), PlacementWorldQueries::isOtherLivingEntity).isEmpty();
    }

    private static boolean isOtherLivingEntity(LivingEntity entity) {
        return entity != CLIENT.player;
    }

    public static boolean hasOtherLivingEntity(BlockPos position, Block block) {
        return hasOtherLivingEntity(position, block.getDefaultState());
    }

    public static boolean intersectsPlayer(BlockPos position) {
        return CLIENT.player != null && position != null && CLIENT.player.getBoundingBox().intersects(new Box(position));
    }

    public static boolean hasBlockingEntity(BlockPos position) {
        if (position == null || CLIENT.world == null) {
            return false;
        }
        return hasBlockingEntity(new Box(position));
    }

    public static boolean hasBlockingEntity(Box bounds) {
        if (bounds == null || CLIENT.world == null) {
            return false;
        }
        for (Entity entity : CLIENT.world.getOtherEntities(null, bounds)) {
            if (blocksPlacement(entity)) {
                return true;
            }
        }
        return false;
    }

    private static boolean blocksPlacement(Entity entity) {
        return entity != null && entity != CLIENT.player && !entity.isRemoved() && entity.intersectionChecked;
    }

    public static BlockPos crosshairAdjacentPosition() {
        HitResult target = CLIENT.crosshairTarget;
        if (target == null || target.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockHitResult blockHit = (BlockHitResult) target;
        return blockHit.getBlockPos().offset(blockHit.getSide());
    }

    public static boolean isReplaceable(BlockPos position) {
        return position != null && CLIENT.world != null && CLIENT.world.getBlockState(position).isReplaceable();
    }

    public static boolean hasEntityInAirAbove(BlockPos position) {
        if (CLIENT.world == null) {
            return false;
        }
        BlockPos above = position.up();
        if (!CLIENT.world.isAir(above)) {
            return false;
        }
        double x = above.getX();
        double y = above.getY();
        double z = above.getZ();
        return !CLIENT.world.getOtherEntities(null, new Box(x, y, z, x + 1.0, y + 2.0, z + 1.0)).isEmpty();
    }

    public static boolean canPlaceStack(ItemStack stack, BlockHitResult hit) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return true;
        }
        ItemPlacementContext context = new ItemPlacementContext(CLIENT.player, Hand.MAIN_HAND, stack, hit);
        BlockState state = blockItem.getBlock().getPlacementState(context);
        return state != null && CLIENT.world.canPlace(state, context.getBlockPos(), ShapeContext.ofPlacement(CLIENT.player));
    }
}
