package dev.echo.client.utils.placement;

import static dev.echo.client.MinecraftAccess.CLIENT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Whole original placement provider assembled from the finished world-02 bodies.
 * Candidate key, marker, cache and initialization domains are recorded in the receipt.
 */
public class BlockPlacement {
    private static final double HIT_INSET = 1.0E-4;
    private static final double FACE_SAMPLE_INSET = 0.49;
    private static final double[][] FACE_SAMPLES = {
            {0.0, 0.0}, {FACE_SAMPLE_INSET, FACE_SAMPLE_INSET}, {FACE_SAMPLE_INSET, -FACE_SAMPLE_INSET},
            {-FACE_SAMPLE_INSET, FACE_SAMPLE_INSET}, {-FACE_SAMPLE_INSET, -FACE_SAMPLE_INSET}
    };
    private static int[] placementMarker;

    static {
        setPlacementMarker(new int[4]);
    }

    public BlockPlacement() {
    }

    public static boolean isBlock(BlockPos position, Block block) {
        return CLIENT.level.getBlockState(position).getBlock() == block;
    }

    /** The second original block-identity query has its own declaration and world read. */
    public static boolean matchesBlock(BlockPos position, Block block) {
        return CLIENT.level.getBlockState(position).getBlock() == block;
    }

    public static boolean isChargedRespawnAnchor(BlockPos position) {
        return matchesBlock(position, Blocks.RESPAWN_ANCHOR)
                && CLIENT.level.getBlockState(position).getValue(RespawnAnchorBlock.CHARGE) != 0;
    }

    public static boolean isUnchargedRespawnAnchor(BlockPos position) {
        return matchesBlock(position, Blocks.RESPAWN_ANCHOR)
                && CLIENT.level.getBlockState(position).getValue(RespawnAnchorBlock.CHARGE) == 0;
    }

    public static boolean isDefaultStateFullCube(Block block) {
        return block.defaultBlockState().isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
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
        return !CLIENT.level.getEntitiesOfClass(LivingEntity.class, new AABB(position), BlockPlacement::isOtherLivingEntity).isEmpty();
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

    public static PlacementTarget find(BlockPos position, boolean requireVisible) {
        return find(position, requireVisible, false);
    }

    public static PlacementTarget find(BlockPos position, boolean requireVisible, boolean allowPartialSupport) {
        return find(position, requireVisible, allowPartialSupport, false);
    }

    public static PlacementTarget find(BlockPos position, boolean requireVisible,
            boolean allowPartialSupport, boolean ignoreEntities) {
        if (position == null || CLIENT.level == null || CLIENT.player == null) {
            return null;
        }
        Vec3 eye = CLIENT.player.getEyePosition();
        PlacementTarget nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        if (requireVisible) {
            PlacementTarget directlyHit = findReplaceableOutline(eye, position);
            if (directlyHit != null) {
                nearest = directlyHit;
                nearestDistance = eye.distanceToSqr(directlyHit.hitResult().getLocation());
            }
        }
        for (Direction neighborDirection : Direction.values()) {
            BlockPos neighbor = position.relative(neighborDirection);
            Direction face = neighborDirection.getOpposite();
            if (!supportsFace(neighbor, face, allowPartialSupport)) {
                continue;
            }
            PlacementTarget candidate = findAgainstNeighbor(eye, position, neighbor, neighborDirection,
                    face, requireVisible, ignoreEntities);
            if (candidate == null) {
                continue;
            }
            double distance = eye.distanceToSqr(candidate.hitResult().getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static PlacementTarget findReplaceableOutline(Vec3 eye, BlockPos position) {
        BlockState state = CLIENT.level.getBlockState(position);
        if (!state.canBeReplaced()) {
            return null;
        }
        VoxelShape shape = state.getShape(CLIENT.level, position);
        if (shape.isEmpty()) {
            return null;
        }
        for (AABB bounds : shape.toAabbs()) {
            Vec3 center = new Vec3(position.getX() + (bounds.minX + bounds.maxX) * 0.5,
                    position.getY() + (bounds.minY + bounds.maxY) * 0.5,
                    position.getZ() + (bounds.minZ + bounds.maxZ) * 0.5);
            BlockHitResult hit = CLIENT.level.clip(new ClipContext(eye, center,
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CLIENT.player));
            if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(position)) {
                return new PlacementTarget(position, hit);
            }
        }
        return null;
    }

    private static boolean hitsEntity(Vec3 start, Vec3 end) {
        return ProjectileUtil.getEntityHitResult(CLIENT.player, start, end, new AABB(start, end).inflate(1.0),
                Entity::isPickable, start.distanceToSqr(end)) != null;
    }

    public static boolean hasSupport(BlockPos position) {
        return hasSupport(position, false);
    }

    public static boolean hasSupport(BlockPos position, boolean allowPartialSupport) {
        if (position == null || CLIENT.level == null) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            if (supportsFace(position.relative(direction), direction.getOpposite(), allowPartialSupport)) {
                return true;
            }
        }
        return false;
    }

    public static boolean supportsFace(BlockPos position, Direction face, boolean allowPartialSupport) {
        BlockState state = CLIENT.level.getBlockState(position);
        if (state.isFaceSturdy(CLIENT.level, position, face)) {
            return true;
        }
        return allowPartialSupport && !state.canBeReplaced()
                && !state.getShape(CLIENT.level, position).isEmpty()
                && !state.getCollisionShape(CLIENT.level, position).isEmpty();
    }

    private static PlacementTarget findAgainstNeighbor(Vec3 eye, BlockPos position, BlockPos neighbor,
            Direction neighborDirection, Direction face, boolean requireVisible, boolean ignoreEntities) {
        VoxelShape outline = CLIENT.level.getBlockState(neighbor).getShape(CLIENT.level, neighbor);
        if (outline.isEmpty()) {
            return null;
        }
        AABB bounds = outline.bounds().move(neighbor);
        double reachSquared = Mth.square(CLIENT.player.blockInteractionRange());
        for (double[] offset : FACE_SAMPLES) {
            Vec3 point = pointOnFace(bounds, face, offset[0], offset[1]);
            BlockHitResult hit = new BlockHitResult(point, face, neighbor, false);
            if (eye.distanceToSqr(point) > reachSquared || !facesEye(hit, eye)) {
                continue;
            }
            if (requireVisible) {
                if (!canSeeHit(eye, hit)) {
                    continue;
                }
                if (!ignoreEntities && hitsEntity(eye, point)) {
                    continue;
                }
            }
            return new PlacementTarget(position, hit);
        }
        return null;
    }

    public static boolean facesEye(BlockHitResult hit, Vec3 eye) {
        if (hit == null || eye == null || CLIENT.level == null) {
            return false;
        }
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        AABB bounds = collisionBounds(hit.getBlockPos());
        if (bounds == null) {
            return false;
        }
        if (bounds.contains(eye)) {
            return true;
        }
        return switch (hit.getDirection()) {
            case NORTH -> eye.z <= bounds.minZ;
            case SOUTH -> eye.z >= bounds.maxZ;
            case EAST -> eye.x >= bounds.maxX;
            case WEST -> eye.x <= bounds.minX;
            case UP -> eye.y >= bounds.maxY;
            case DOWN -> eye.y <= bounds.minY;
            default -> throw new MatchException(null, null);
        };
    }

    private static AABB collisionBounds(BlockPos position) {
        BlockState state = CLIENT.level.getBlockState(position);
        VoxelShape shape = state.getCollisionShape(CLIENT.level, position);
        return shape.isEmpty() ? new AABB(position) : shape.bounds().move(position);
    }

    public static boolean canSeeHit(Vec3 eye, BlockHitResult expected) {
        Vec3 inside = expected.getLocation().relative(expected.getDirection().getOpposite(), HIT_INSET);
        BlockHitResult actual = CLIENT.level.clip(new ClipContext(eye, inside,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CLIENT.player));
        return actual.getType() == HitResult.Type.BLOCK
                && actual.getBlockPos().equals(expected.getBlockPos())
                && actual.getDirection() == expected.getDirection();
    }

    private static Vec3 pointOnFace(AABB bounds, Direction face, double u, double v) {
        Vec3 center = new Vec3(
                face == Direction.WEST ? bounds.minX : face == Direction.EAST ? bounds.maxX : (bounds.minX + bounds.maxX) * 0.5,
                face == Direction.DOWN ? bounds.minY : face == Direction.UP ? bounds.maxY : (bounds.minY + bounds.maxY) * 0.5,
                face == Direction.NORTH ? bounds.minZ : face == Direction.SOUTH ? bounds.maxZ : (bounds.minZ + bounds.maxZ) * 0.5);
        return switch (face.getAxis()) {
            case X -> center.add(0.0, u * bounds.getYsize(), v * bounds.getZsize());
            case Y -> center.add(u * bounds.getXsize(), 0.0, v * bounds.getZsize());
            case Z -> center.add(u * bounds.getXsize(), v * bounds.getYsize(), 0.0);
            default -> throw new MatchException(null, null);
        };
    }

    public static void setPlacementMarker(int[] marker) {
        placementMarker = marker;
    }

    public static int[] getPlacementMarker() {
        return placementMarker;
    }
}
