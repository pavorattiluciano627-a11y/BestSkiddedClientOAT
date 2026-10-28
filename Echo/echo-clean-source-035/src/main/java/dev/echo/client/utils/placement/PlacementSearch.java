package dev.echo.client.utils.placement;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.Event;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Shared placement geometry/planning in the original null-marker control domain. */
public abstract class PlacementSearch implements MinecraftAccess {
    public abstract List<PlacementFilter> plan(LivingEntity target, Vec3d displacement);

    public boolean allowsPartialSupport() {
        return false;
    }

    protected abstract Predicate<ItemStack> stackFilter();

    protected boolean includeRaisedPathPosition() {
        return true;
    }

    public List<PlacementFilter> planPredicted(LivingEntity target, Vec3d displacement) {
        Vec3d direction = new Vec3d(displacement.x, 0.0, displacement.z).normalize();
        double pathLength = displacement.horizontalLength();
        Box original = target.getBoundingBox().contract(1.0E-4);
        int supportY = PlacementBounds.around(target, Vec3d.ZERO, PlacementWorldQueries::isReplaceable).minY();
        Vec3d playerPosition = CLIENT.player.getEntityPos();
        List<BlockPos> candidates = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (double distance = 0.25; distance <= pathLength + 5.0; distance += 0.25) {
            List<BlockPos> frontier = newlyCoveredCells(original, original.offset(direction.multiply(distance)), supportY);
            BlockPos obstruction = frontier.stream().filter(position -> !PlacementWorldQueries.isReplaceable(position))
                    .findFirst().orElse(null);
            if (obstruction != null) {
                if (includeRaisedPathPosition() && PlacementWorldQueries.isReplaceable(obstruction.up())) {
                    return List.of(new PlacementFilter(obstruction.up(), stackFilter()));
                }
                List<BlockPos> forward = frontier.stream().filter(PlacementWorldQueries::isReplaceable)
                        .filter(position -> (position.getX() - obstruction.getX()) * direction.x
                                + (position.getZ() - obstruction.getZ()) * direction.z > -0.001)
                        .toList();
                return pathPlacements(forward, playerPosition);
            }
            for (BlockPos position : frontier) {
                if (!seen.add(position)) {
                    continue;
                }
                if (distance >= pathLength && PlacementDistances.horizontalSquaredDistance(position, playerPosition) < 2.25) {
                    continue;
                }
                candidates.add(position);
            }
        }
        List<PlacementFilter> result = candidates.isEmpty() ? plan(target, Vec3d.ZERO) : pathPlacements(candidates, playerPosition);
        if (Event.getEventCounterFallbackValue() == 0) {
            PlacementStackPredicates.setTypeCheckBypassMarker("wF4Oxb");
        }
        return result;
    }

    private List<PlacementFilter> pathPlacements(List<BlockPos> positions, Vec3d origin) {
        List<PlacementFilter> result = new ArrayList<>();
        positions.stream().sorted(Comparator.comparingDouble(position -> PlacementDistances.horizontalSquaredDistance(position, origin)))
                .forEach(position -> {
                    result.add(new PlacementFilter(position, stackFilter()));
                    if (includeRaisedPathPosition()) {
                        result.add(new PlacementFilter(position.up(), stackFilter()));
                    }
                });
        return result;
    }

    private static List<BlockPos> newlyCoveredCells(Box original, Box shifted, int y) {
        List<BlockPos> result = new ArrayList<>();
        for (int x = MathHelper.floor(shifted.minX); x <= MathHelper.floor(shifted.maxX); x++) {
            for (int z = MathHelper.floor(shifted.minZ); z <= MathHelper.floor(shifted.maxZ); z++) {
                boolean insideOriginal = x >= MathHelper.floor(original.minX) && x <= MathHelper.floor(original.maxX)
                        && z >= MathHelper.floor(original.minZ) && z <= MathHelper.floor(original.maxZ);
                if (!insideOriginal) {
                    result.add(new BlockPos(x, y, z));
                }
            }
        }
        return result;
    }

    protected static List<BlockPos> perimeter(PlacementBounds bounds) {
        List<BlockPos> result = new ArrayList<>();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            result.add(new BlockPos(x, bounds.minY(), bounds.minZ() - 1));
            result.add(new BlockPos(x, bounds.minY(), bounds.maxZ() + 1));
        }
        for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
            result.add(new BlockPos(bounds.minX() - 1, bounds.minY(), z));
            result.add(new BlockPos(bounds.maxX() + 1, bounds.minY(), z));
        }
        return result;
    }

    protected static List<BlockPos> perimeterAt(PlacementBounds bounds, int y) {
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos base : perimeter(bounds)) {
            BlockPos position = base.withY(y);
            if (PlacementWorldQueries.isReplaceable(position)) {
                result.add(position);
            }
        }
        return result;
    }

    protected static List<BlockPos> roof(PlacementBounds bounds) {
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos position : BlockPos.iterate(bounds.minX(), bounds.aboveTop(), bounds.minZ(),
                bounds.maxX(), bounds.aboveTop(), bounds.maxZ())) {
            if (PlacementWorldQueries.isReplaceable(position)) {
                result.add(position.toImmutable());
            }
        }
        return result;
    }

    protected boolean anyPlaceable(List<BlockPos> positions) {
        return positions.stream().anyMatch(position -> PlacementTargets.find(position, true, allowsPartialSupport(), true) != null);
    }

    protected static boolean hasAdjacentVisibleFace(BlockPos position, List<BlockPos> adjacent, Vec3d eye) {
        for (Direction side : Direction.Type.HORIZONTAL) {
            if (!adjacent.contains(position.offset(side))) {
                continue;
            }
            Vec3d point = Vec3d.ofCenter(position).offset(side, 0.5);
            if (PlacementRaycasts.facesEye(new BlockHitResult(point, side, position, false), eye)) {
                return true;
            }
        }
        return false;
    }

    protected static void append(List<PlacementFilter> result, List<BlockPos> positions, Predicate<ItemStack> filter) {
        for (BlockPos position : positions) {
            result.add(new PlacementFilter(position, filter));
        }
    }
}
