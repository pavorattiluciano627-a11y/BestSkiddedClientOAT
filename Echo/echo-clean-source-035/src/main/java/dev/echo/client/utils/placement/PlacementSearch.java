package dev.echo.client.utils.placement;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.Event;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Shared placement geometry/planning in the original null-marker control domain. */
public abstract class PlacementSearch implements MinecraftAccess {
    public abstract List<PlacementFilter> plan(LivingEntity target, Vec3 displacement);

    public boolean allowsPartialSupport() {
        return false;
    }

    protected abstract Predicate<ItemStack> stackFilter();

    protected boolean includeRaisedPathPosition() {
        return true;
    }

    public List<PlacementFilter> planPredicted(LivingEntity target, Vec3 displacement) {
        Vec3 direction = new Vec3(displacement.x, 0.0, displacement.z).normalize();
        double pathLength = displacement.horizontalDistance();
        AABB original = target.getBoundingBox().deflate(1.0E-4);
        int supportY = PlacementBounds.around(target, Vec3.ZERO, PlacementWorldQueries::isReplaceable).minY();
        Vec3 playerPosition = CLIENT.player.position();
        List<BlockPos> candidates = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (double distance = 0.25; distance <= pathLength + 5.0; distance += 0.25) {
            List<BlockPos> frontier = newlyCoveredCells(original, original.move(direction.scale(distance)), supportY);
            BlockPos obstruction = frontier.stream().filter(position -> !PlacementWorldQueries.isReplaceable(position))
                    .findFirst().orElse(null);
            if (obstruction != null) {
                if (includeRaisedPathPosition() && PlacementWorldQueries.isReplaceable(obstruction.above())) {
                    return List.of(new PlacementFilter(obstruction.above(), stackFilter()));
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
        List<PlacementFilter> result = candidates.isEmpty() ? plan(target, Vec3.ZERO) : pathPlacements(candidates, playerPosition);
        if (Event.getEventCounterFallbackValue() == 0) {
            PlacementStackPredicates.setTypeCheckBypassMarker("wF4Oxb");
        }
        return result;
    }

    private List<PlacementFilter> pathPlacements(List<BlockPos> positions, Vec3 origin) {
        List<PlacementFilter> result = new ArrayList<>();
        positions.stream().sorted(Comparator.comparingDouble(position -> PlacementDistances.horizontalSquaredDistance(position, origin)))
                .forEach(position -> {
                    result.add(new PlacementFilter(position, stackFilter()));
                    if (includeRaisedPathPosition()) {
                        result.add(new PlacementFilter(position.above(), stackFilter()));
                    }
                });
        return result;
    }

    private static List<BlockPos> newlyCoveredCells(AABB original, AABB shifted, int y) {
        List<BlockPos> result = new ArrayList<>();
        for (int x = Mth.floor(shifted.minX); x <= Mth.floor(shifted.maxX); x++) {
            for (int z = Mth.floor(shifted.minZ); z <= Mth.floor(shifted.maxZ); z++) {
                boolean insideOriginal = x >= Mth.floor(original.minX) && x <= Mth.floor(original.maxX)
                        && z >= Mth.floor(original.minZ) && z <= Mth.floor(original.maxZ);
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
            BlockPos position = base.atY(y);
            if (PlacementWorldQueries.isReplaceable(position)) {
                result.add(position);
            }
        }
        return result;
    }

    protected static List<BlockPos> roof(PlacementBounds bounds) {
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos position : BlockPos.betweenClosed(bounds.minX(), bounds.aboveTop(), bounds.minZ(),
                bounds.maxX(), bounds.aboveTop(), bounds.maxZ())) {
            if (PlacementWorldQueries.isReplaceable(position)) {
                result.add(position.immutable());
            }
        }
        return result;
    }

    protected boolean anyPlaceable(List<BlockPos> positions) {
        return positions.stream().anyMatch(position -> PlacementTargets.find(position, true, allowsPartialSupport(), true) != null);
    }

    protected static boolean hasAdjacentVisibleFace(BlockPos position, List<BlockPos> adjacent, Vec3 eye) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (!adjacent.contains(position.relative(side))) {
                continue;
            }
            Vec3 point = Vec3.atCenterOf(position).relative(side, 0.5);
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
