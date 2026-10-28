package dev.echo.client.feature.utility;

import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Anti Trap's block/entity interaction policy and crosshair repicking algorithm.
 * The feature's live settings are supplied at the event boundary; this class does
 * not register another feature or replace the shared setting model.
 */
public final class AntiTrapTargeting {
    private AntiTrapTargeting() {
    }

    /** Tests the two sign families followed by the user-selected block entries.
     * @param state block under consideration
     * @param signs whether standing and wall signs are blocked
     * @param hangingSigns whether ceiling and wall hanging signs are blocked
     * @param selectedBlocks selection predicate from the block-list setting
     * @return whether the block should be skipped
     */
    public static boolean blocksInteraction(BlockState state, boolean signs,
                                            boolean hangingSigns, Predicate<Block> selectedBlocks) {
        if (state == null || state.isAir()) {
            return false;
        }
        if (signs && (state.is(BlockTags.STANDING_SIGNS) || state.is(BlockTags.WALL_SIGNS))) {
            return true;
        }
        if (hangingSigns && (state.is(BlockTags.CEILING_HANGING_SIGNS)
                || state.is(BlockTags.WALL_HANGING_SIGNS))) {
            return true;
        }
        return selectedBlocks.test(state.getBlock());
    }

    /** Tests the five independently selectable entity kinds in their original order.
     * @param type entity type, or {@code null}
     * @param armorStands armor-stand setting
     * @param itemFrames item-frame setting
     * @param glowFrames glow-frame setting
     * @param paintings painting setting
     * @param chestMinecarts chest-minecart setting
     * @return whether interaction with this type is blocked
     */
    public static boolean blocksEntity(EntityType<?> type, boolean armorStands, boolean itemFrames,
                                       boolean glowFrames, boolean paintings, boolean chestMinecarts) {
        if (type == null) {
            return false;
        }
        return (armorStands && type == EntityTypes.ARMOR_STAND)
                || (itemFrames && type == EntityTypes.ITEM_FRAME)
                || (glowFrames && type == EntityTypes.GLOW_ITEM_FRAME)
                || (paintings && type == EntityTypes.PAINTING)
                || (chestMinecarts && type == EntityTypes.CHEST_MINECART);
    }

    /** Determines whether a block-interaction event should be cancelled.
     * @param client current client
     * @param noInteract master interaction setting
     * @param hit event's block hit
     * @param blockedBlocks block policy
     * @return whether the event should be cancelled
     */
    public static boolean shouldCancelBlockInteraction(Minecraft client, boolean noInteract,
                                                       BlockHitResult hit, Predicate<BlockState> blockedBlocks) {
        return noInteract && client.player != null && client.level != null && hit != null
                && hit.getType() == HitResult.Type.BLOCK
                && blockedBlocks.test(client.level.getBlockState(hit.getBlockPos()));
    }

    /** Determines whether an entity-interaction event should be cancelled.
     * @param noInteract master interaction setting
     * @param entity event target
     * @param blockedTypes entity policy
     * @return whether the event should be cancelled
     */
    public static boolean shouldCancelEntityInteraction(boolean noInteract, Entity entity,
                                                        Predicate<EntityType<?>> blockedTypes) {
        return noInteract && entity != null && blockedTypes.test(entity.getType());
    }

    /** Replaces a crosshair hit obstructed by a configured block.
     * @param client current client
     * @param noInteract master interaction setting
     * @param blockedBlocks block policy
     * @param blockedTypes entity policy
     */
    public static void updateCrosshair(Minecraft client, boolean noInteract,
                                      Predicate<BlockState> blockedBlocks,
                                      Predicate<EntityType<?>> blockedTypes) {
        if (!noInteract || client.player == null || client.level == null
                || !(client.hitResult instanceof BlockHitResult blockHit)
                || !blockedBlocks.test(client.level.getBlockState(blockHit.getBlockPos()))) {
            return;
        }
        HitResult replacement = pickPastBlockedBlocks(client, blockedBlocks, blockedTypes);
        client.hitResult = replacement;
        client.crosshairPickEntity = replacement instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
    }

    /** Picks the nearest permitted block or entity, respecting their distinct reaches.
     * @param client client with a live player and world
     * @param blockedBlocks block policy
     * @param blockedTypes entity policy
     * @return replacement hit, including a miss when the selected hit is out of reach
     */
    public static HitResult pickPastBlockedBlocks(Minecraft client,
                                                  Predicate<BlockState> blockedBlocks,
                                                  Predicate<EntityType<?>> blockedTypes) {
        LocalPlayer player = client.player;
        float tickProgress = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(tickProgress);
        double blockReach = player.blockInteractionRange();
        double entityReach = player.entityInteractionRange();
        double reach = Math.max(blockReach, entityReach);
        Vec3 end = eye.add(look.scale(reach));
        BlockHitResult blockHit = raycastPastBlockedBlocks(client.level, eye, end, player, blockedBlocks);
        double maximumDistanceSquared = reach * reach;
        double entityRayLength = reach;
        if (blockHit.getType() != HitResult.Type.MISS) {
            maximumDistanceSquared = eye.distanceToSqr(blockHit.getLocation());
            entityRayLength = Math.sqrt(maximumDistanceSquared);
        }
        Vec3 entityRayEnd = eye.add(look.scale(entityRayLength));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(entityRayLength)).inflate(1.0, 1.0, 1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, entityRayEnd, searchBox,
                entity -> EntitySelector.CAN_BE_PICKED.test(entity) && !blockedTypes.test(entity.getType()),
                maximumDistanceSquared);
        if (entityHit != null && entityHit.getLocation().distanceToSqr(eye) < maximumDistanceSquared) {
            return limitToReach(entityHit, eye, entityReach);
        }
        if (blockHit.getType() != HitResult.Type.MISS) {
            return limitToReach(blockHit, eye, blockReach);
        }
        return miss(end, look);
    }

    /** Advances through at most sixteen configured block hits.
     * @param world current world
     * @param start original eye position
     * @param end ray endpoint
     * @param player raycasting entity
     * @param blockedBlocks blocks to skip
     * @return first permitted hit, or a miss at the original endpoint
     */
    public static BlockHitResult raycastPastBlockedBlocks(ClientLevel world, Vec3 start, Vec3 end,
                                                          LocalPlayer player,
                                                          Predicate<BlockState> blockedBlocks) {
        Vec3 cursor = start;
        for (int attempt = 0; attempt < 16; attempt++) {
            BlockHitResult hit = world.clip(new ClipContext(cursor, end,
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) {
                return miss(end, end.subtract(start));
            }
            if (!blockedBlocks.test(world.getBlockState(hit.getBlockPos()))) {
                return hit;
            }
            cursor = advancePastHit(hit.getLocation(), start, end);
        }
        return miss(end, end.subtract(start));
    }

    /** Advances a hit by the original 0.001-block epsilon along the original ray.
     * @param hit position to advance
     * @param start original ray start
     * @param end original ray end
     * @return advanced hit, or the same object for a zero-length ray
     */
    public static Vec3 advancePastHit(Vec3 hit, Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() == 0.0) {
            return hit;
        }
        return hit.add(direction.normalize().scale(0.001));
    }

    /** Converts an out-of-range result to a miss at that result's position.
     * @param hit candidate result
     * @param eye original eye position
     * @param reach range for this result kind
     * @return the original result when in range; otherwise a miss
     */
    public static HitResult limitToReach(HitResult hit, Vec3 eye, double reach) {
        Vec3 position = hit.getLocation();
        if (position.closerThan(eye, reach)) {
            return hit;
        }
        return miss(position, position.subtract(eye));
    }

    /** Creates the directional miss used by all Anti Trap raycast exits.
     * @param position miss position
     * @param direction look direction
     * @return a Minecraft block miss
     */
    public static BlockHitResult miss(Vec3 position, Vec3 direction) {
        return BlockHitResult.miss(position,
                Direction.getApproximateNearest(direction.x, direction.y, direction.z), BlockPos.containing(position));
    }
}
