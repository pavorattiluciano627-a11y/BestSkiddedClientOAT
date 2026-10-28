package dev.echo.client.utils.placement;

import dev.echo.client.MinecraftAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Complete source unit for the recovered placement queries and main-hand interaction.
 * Original dispatch and guard applicability are still tracked as finite obligations.
 */
public final class PlacementWorldQueries implements MinecraftAccess {
    private PlacementWorldQueries() {
    }

    public static boolean isBlock(BlockPos position, Block block) {
        return CLIENT.level.getBlockState(position).getBlock() == block;
    }

    public static boolean isChargedRespawnAnchor(BlockPos position) {
        return isBlock(position, Blocks.RESPAWN_ANCHOR)
                && CLIENT.level.getBlockState(position).getValue(RespawnAnchorBlock.CHARGE) != 0;
    }

    public static boolean isUnchargedRespawnAnchor(BlockPos position) {
        return isBlock(position, Blocks.RESPAWN_ANCHOR)
                && CLIENT.level.getBlockState(position).getValue(RespawnAnchorBlock.CHARGE) == 0;
    }

    public static boolean isEndCrystalBase(BlockState state) {
        return state != null && (state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK));
    }

    public static BlockPos placementPosition(BlockHitResult hit) {
        return new BlockPlaceContext(CLIENT.player, InteractionHand.MAIN_HAND, CLIENT.player.getMainHandItem(), hit).getClickedPos();
    }

    public static InteractionResult interactMainHand(BlockHitResult hit, boolean swing) {
        if (CLIENT.player == null || CLIENT.gameMode == null) {
            return InteractionResult.PASS;
        }
        InteractionResult result = CLIENT.gameMode.useItemOn(CLIENT.player, InteractionHand.MAIN_HAND, hit);
        if (result.consumesAction() && swing && result instanceof InteractionResult.Success success) {
            InteractionResult.SwingSource source = success.swingSource();
            if (source == InteractionResult.SwingSource.CLIENT || source == InteractionResult.SwingSource.SERVER) {
                CLIENT.player.swing(InteractionHand.MAIN_HAND);
            }
        }
        return result;
    }

    public static boolean hasOtherLivingEntity(BlockPos position, BlockState state) {
        if (CLIENT.level == null || position == null || state == null) {
            return false;
        }
        // The supplied state is only null-checked in the original method.
        return !CLIENT.level.getEntitiesOfClass(LivingEntity.class, new AABB(position), PlacementWorldQueries::isOtherLivingEntity).isEmpty();
    }

    private static boolean isOtherLivingEntity(LivingEntity entity) {
        return entity != CLIENT.player;
    }

    public static boolean hasOtherLivingEntity(BlockPos position, Block block) {
        return hasOtherLivingEntity(position, block.defaultBlockState());
    }

    public static boolean intersectsPlayer(BlockPos position) {
        return CLIENT.player != null && position != null && CLIENT.player.getBoundingBox().intersects(new AABB(position));
    }

    public static boolean hasBlockingEntity(BlockPos position) {
        if (position == null || CLIENT.level == null) {
            return false;
        }
        return hasBlockingEntity(new AABB(position));
    }

    public static boolean hasBlockingEntity(AABB bounds) {
        if (bounds == null || CLIENT.level == null) {
            return false;
        }
        for (Entity entity : CLIENT.level.getEntities(null, bounds)) {
            if (blocksPlacement(entity)) {
                return true;
            }
        }
        return false;
    }

    private static boolean blocksPlacement(Entity entity) {
        return entity != null && entity != CLIENT.player && !entity.isRemoved() && entity.blocksBuilding;
    }

    public static BlockPos crosshairAdjacentPosition() {
        HitResult target = CLIENT.hitResult;
        if (target == null || target.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockHitResult blockHit = (BlockHitResult) target;
        return blockHit.getBlockPos().relative(blockHit.getDirection());
    }

    public static boolean isReplaceable(BlockPos position) {
        return position != null && CLIENT.level != null && CLIENT.level.getBlockState(position).canBeReplaced();
    }

    public static boolean hasEntityInAirAbove(BlockPos position) {
        if (CLIENT.level == null) {
            return false;
        }
        BlockPos above = position.above();
        if (!CLIENT.level.isEmptyBlock(above)) {
            return false;
        }
        double x = above.getX();
        double y = above.getY();
        double z = above.getZ();
        return !CLIENT.level.getEntities(null, new AABB(x, y, z, x + 1.0, y + 2.0, z + 1.0)).isEmpty();
    }

    public static boolean canPlaceStack(ItemStack stack, BlockHitResult hit) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return true;
        }
        BlockPlaceContext context = new BlockPlaceContext(CLIENT.player, InteractionHand.MAIN_HAND, stack, hit);
        BlockState state = blockItem.getBlock().getStateForPlacement(context);
        return state != null && CLIENT.level.isUnobstructed(state, context.getClickedPos(), CollisionContext.placementContext(CLIENT.player));
    }
}
