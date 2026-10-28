package dev.echo.client.feature.utility;

import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

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
        if (signs && (state.isIn(BlockTags.STANDING_SIGNS) || state.isIn(BlockTags.WALL_SIGNS))) {
            return true;
        }
        if (hangingSigns && (state.isIn(BlockTags.CEILING_HANGING_SIGNS)
                || state.isIn(BlockTags.WALL_HANGING_SIGNS))) {
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
        return (armorStands && type == EntityType.ARMOR_STAND)
                || (itemFrames && type == EntityType.ITEM_FRAME)
                || (glowFrames && type == EntityType.GLOW_ITEM_FRAME)
                || (paintings && type == EntityType.PAINTING)
                || (chestMinecarts && type == EntityType.CHEST_MINECART);
    }

    /** Determines whether a block-interaction event should be cancelled.
     * @param client current client
     * @param noInteract master interaction setting
     * @param hit event's block hit
     * @param blockedBlocks block policy
     * @return whether the event should be cancelled
     */
    public static boolean shouldCancelBlockInteraction(MinecraftClient client, boolean noInteract,
                                                       BlockHitResult hit, Predicate<BlockState> blockedBlocks) {
        return noInteract && client.player != null && client.world != null && hit != null
                && hit.getType() == HitResult.Type.BLOCK
                && blockedBlocks.test(client.world.getBlockState(hit.getBlockPos()));
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
    public static void updateCrosshair(MinecraftClient client, boolean noInteract,
                                      Predicate<BlockState> blockedBlocks,
                                      Predicate<EntityType<?>> blockedTypes) {
        if (!noInteract || client.player == null || client.world == null
                || !(client.crosshairTarget instanceof BlockHitResult blockHit)
                || !blockedBlocks.test(client.world.getBlockState(blockHit.getBlockPos()))) {
            return;
        }
        HitResult replacement = pickPastBlockedBlocks(client, blockedBlocks, blockedTypes);
        client.crosshairTarget = replacement;
        client.targetedEntity = replacement instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
    }

    /** Picks the nearest permitted block or entity, respecting their distinct reaches.
     * @param client client with a live player and world
     * @param blockedBlocks block policy
     * @param blockedTypes entity policy
     * @return replacement hit, including a miss when the selected hit is out of reach
     */
    public static HitResult pickPastBlockedBlocks(MinecraftClient client,
                                                  Predicate<BlockState> blockedBlocks,
                                                  Predicate<EntityType<?>> blockedTypes) {
        ClientPlayerEntity player = client.player;
        float tickProgress = client.getRenderTickCounter().getTickProgress(false);
        Vec3d eye = player.getCameraPosVec(1.0F);
        Vec3d look = player.getRotationVec(tickProgress);
        double blockReach = player.getBlockInteractionRange();
        double entityReach = player.getEntityInteractionRange();
        double reach = Math.max(blockReach, entityReach);
        Vec3d end = eye.add(look.multiply(reach));
        BlockHitResult blockHit = raycastPastBlockedBlocks(client.world, eye, end, player, blockedBlocks);
        double maximumDistanceSquared = reach * reach;
        double entityRayLength = reach;
        if (blockHit.getType() != HitResult.Type.MISS) {
            maximumDistanceSquared = eye.squaredDistanceTo(blockHit.getPos());
            entityRayLength = Math.sqrt(maximumDistanceSquared);
        }
        Vec3d entityRayEnd = eye.add(look.multiply(entityRayLength));
        Box searchBox = player.getBoundingBox().stretch(look.multiply(entityRayLength)).expand(1.0, 1.0, 1.0);
        EntityHitResult entityHit = ProjectileUtil.raycast(player, eye, entityRayEnd, searchBox,
                entity -> EntityPredicates.CAN_HIT.test(entity) && !blockedTypes.test(entity.getType()),
                maximumDistanceSquared);
        if (entityHit != null && entityHit.getPos().squaredDistanceTo(eye) < maximumDistanceSquared) {
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
    public static BlockHitResult raycastPastBlockedBlocks(ClientWorld world, Vec3d start, Vec3d end,
                                                          ClientPlayerEntity player,
                                                          Predicate<BlockState> blockedBlocks) {
        Vec3d cursor = start;
        for (int attempt = 0; attempt < 16; attempt++) {
            BlockHitResult hit = world.raycast(new RaycastContext(cursor, end,
                    RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) {
                return miss(end, end.subtract(start));
            }
            if (!blockedBlocks.test(world.getBlockState(hit.getBlockPos()))) {
                return hit;
            }
            cursor = advancePastHit(hit.getPos(), start, end);
        }
        return miss(end, end.subtract(start));
    }

    /** Advances a hit by the original 0.001-block epsilon along the original ray.
     * @param hit position to advance
     * @param start original ray start
     * @param end original ray end
     * @return advanced hit, or the same object for a zero-length ray
     */
    public static Vec3d advancePastHit(Vec3d hit, Vec3d start, Vec3d end) {
        Vec3d direction = end.subtract(start);
        if (direction.lengthSquared() == 0.0) {
            return hit;
        }
        return hit.add(direction.normalize().multiply(0.001));
    }

    /** Converts an out-of-range result to a miss at that result's position.
     * @param hit candidate result
     * @param eye original eye position
     * @param reach range for this result kind
     * @return the original result when in range; otherwise a miss
     */
    public static HitResult limitToReach(HitResult hit, Vec3d eye, double reach) {
        Vec3d position = hit.getPos();
        if (position.isInRange(eye, reach)) {
            return hit;
        }
        return miss(position, position.subtract(eye));
    }

    /** Creates the directional miss used by all Anti Trap raycast exits.
     * @param position miss position
     * @param direction look direction
     * @return a Minecraft block miss
     */
    public static BlockHitResult miss(Vec3d position, Vec3d direction) {
        return BlockHitResult.createMissed(position,
                Direction.getFacing(direction.x, direction.y, direction.z), BlockPos.ofFloored(position));
    }
}
