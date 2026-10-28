package dev.echo.client.utils.placement;

import static dev.echo.client.MinecraftAccess.CLIENT;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.RaycastContext;

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
        return CLIENT.world.getBlockState(position).getBlock() == block;
    }

    /** The second original block-identity query has its own declaration and world read. */
    public static boolean matchesBlock(BlockPos position, Block block) {
        return CLIENT.world.getBlockState(position).getBlock() == block;
    }

    public static boolean isChargedRespawnAnchor(BlockPos position) {
        return matchesBlock(position, Blocks.RESPAWN_ANCHOR)
                && CLIENT.world.getBlockState(position).get(RespawnAnchorBlock.CHARGES) != 0;
    }

    public static boolean isUnchargedRespawnAnchor(BlockPos position) {
        return matchesBlock(position, Blocks.RESPAWN_ANCHOR)
                && CLIENT.world.getBlockState(position).get(RespawnAnchorBlock.CHARGES) == 0;
    }

    public static boolean isDefaultStateFullCube(Block block) {
        return block.getDefaultState().isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
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
        return !CLIENT.world.getEntitiesByClass(LivingEntity.class, new Box(position), BlockPlacement::isOtherLivingEntity).isEmpty();
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

    public static PlacementTarget find(BlockPos position, boolean requireVisible) {
        return find(position, requireVisible, false);
    }

    public static PlacementTarget find(BlockPos position, boolean requireVisible, boolean allowPartialSupport) {
        return find(position, requireVisible, allowPartialSupport, false);
    }

    public static PlacementTarget find(BlockPos position, boolean requireVisible,
            boolean allowPartialSupport, boolean ignoreEntities) {
        if (position == null || CLIENT.world == null || CLIENT.player == null) {
            return null;
        }
        Vec3d eye = CLIENT.player.getEyePos();
        PlacementTarget nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        if (requireVisible) {
            PlacementTarget directlyHit = findReplaceableOutline(eye, position);
            if (directlyHit != null) {
                nearest = directlyHit;
                nearestDistance = eye.squaredDistanceTo(directlyHit.hitResult().getPos());
            }
        }
        for (Direction neighborDirection : Direction.values()) {
            BlockPos neighbor = position.offset(neighborDirection);
            Direction face = neighborDirection.getOpposite();
            if (!supportsFace(neighbor, face, allowPartialSupport)) {
                continue;
            }
            PlacementTarget candidate = findAgainstNeighbor(eye, position, neighbor, neighborDirection,
                    face, requireVisible, ignoreEntities);
            if (candidate == null) {
                continue;
            }
            double distance = eye.squaredDistanceTo(candidate.hitResult().getPos());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static PlacementTarget findReplaceableOutline(Vec3d eye, BlockPos position) {
        BlockState state = CLIENT.world.getBlockState(position);
        if (!state.isReplaceable()) {
            return null;
        }
        VoxelShape shape = state.getOutlineShape(CLIENT.world, position);
        if (shape.isEmpty()) {
            return null;
        }
        for (Box bounds : shape.getBoundingBoxes()) {
            Vec3d center = new Vec3d(position.getX() + (bounds.minX + bounds.maxX) * 0.5,
                    position.getY() + (bounds.minY + bounds.maxY) * 0.5,
                    position.getZ() + (bounds.minZ + bounds.maxZ) * 0.5);
            BlockHitResult hit = CLIENT.world.raycast(new RaycastContext(eye, center,
                    RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, CLIENT.player));
            if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(position)) {
                return new PlacementTarget(position, hit);
            }
        }
        return null;
    }

    private static boolean hitsEntity(Vec3d start, Vec3d end) {
        return ProjectileUtil.raycast(CLIENT.player, start, end, new Box(start, end).expand(1.0),
                Entity::canHit, start.squaredDistanceTo(end)) != null;
    }

    public static boolean hasSupport(BlockPos position) {
        return hasSupport(position, false);
    }

    public static boolean hasSupport(BlockPos position, boolean allowPartialSupport) {
        if (position == null || CLIENT.world == null) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            if (supportsFace(position.offset(direction), direction.getOpposite(), allowPartialSupport)) {
                return true;
            }
        }
        return false;
    }

    public static boolean supportsFace(BlockPos position, Direction face, boolean allowPartialSupport) {
        BlockState state = CLIENT.world.getBlockState(position);
        if (state.isSideSolidFullSquare(CLIENT.world, position, face)) {
            return true;
        }
        return allowPartialSupport && !state.isReplaceable()
                && !state.getOutlineShape(CLIENT.world, position).isEmpty()
                && !state.getCollisionShape(CLIENT.world, position).isEmpty();
    }

    private static PlacementTarget findAgainstNeighbor(Vec3d eye, BlockPos position, BlockPos neighbor,
            Direction neighborDirection, Direction face, boolean requireVisible, boolean ignoreEntities) {
        VoxelShape outline = CLIENT.world.getBlockState(neighbor).getOutlineShape(CLIENT.world, neighbor);
        if (outline.isEmpty()) {
            return null;
        }
        Box bounds = outline.getBoundingBox().offset(neighbor);
        double reachSquared = MathHelper.square(CLIENT.player.getBlockInteractionRange());
        for (double[] offset : FACE_SAMPLES) {
            Vec3d point = pointOnFace(bounds, face, offset[0], offset[1]);
            BlockHitResult hit = new BlockHitResult(point, face, neighbor, false);
            if (eye.squaredDistanceTo(point) > reachSquared || !facesEye(hit, eye)) {
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

    public static boolean facesEye(BlockHitResult hit, Vec3d eye) {
        if (hit == null || eye == null || CLIENT.world == null) {
            return false;
        }
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        Box bounds = collisionBounds(hit.getBlockPos());
        if (bounds == null) {
            return false;
        }
        if (bounds.contains(eye)) {
            return true;
        }
        return switch (hit.getSide()) {
            case NORTH -> eye.z <= bounds.minZ;
            case SOUTH -> eye.z >= bounds.maxZ;
            case EAST -> eye.x >= bounds.maxX;
            case WEST -> eye.x <= bounds.minX;
            case UP -> eye.y >= bounds.maxY;
            case DOWN -> eye.y <= bounds.minY;
            default -> throw new MatchException(null, null);
        };
    }

    private static Box collisionBounds(BlockPos position) {
        BlockState state = CLIENT.world.getBlockState(position);
        VoxelShape shape = state.getCollisionShape(CLIENT.world, position);
        return shape.isEmpty() ? new Box(position) : shape.getBoundingBox().offset(position);
    }

    public static boolean canSeeHit(Vec3d eye, BlockHitResult expected) {
        Vec3d inside = expected.getPos().offset(expected.getSide().getOpposite(), HIT_INSET);
        BlockHitResult actual = CLIENT.world.raycast(new RaycastContext(eye, inside,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, CLIENT.player));
        return actual.getType() == HitResult.Type.BLOCK
                && actual.getBlockPos().equals(expected.getBlockPos())
                && actual.getSide() == expected.getSide();
    }

    private static Vec3d pointOnFace(Box bounds, Direction face, double u, double v) {
        Vec3d center = new Vec3d(
                face == Direction.WEST ? bounds.minX : face == Direction.EAST ? bounds.maxX : (bounds.minX + bounds.maxX) * 0.5,
                face == Direction.DOWN ? bounds.minY : face == Direction.UP ? bounds.maxY : (bounds.minY + bounds.maxY) * 0.5,
                face == Direction.NORTH ? bounds.minZ : face == Direction.SOUTH ? bounds.maxZ : (bounds.minZ + bounds.maxZ) * 0.5);
        return switch (face.getAxis()) {
            case X -> center.add(0.0, u * bounds.getLengthY(), v * bounds.getLengthZ());
            case Y -> center.add(u * bounds.getLengthX(), 0.0, v * bounds.getLengthZ());
            case Z -> center.add(u * bounds.getLengthX(), v * bounds.getLengthY(), 0.0);
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
