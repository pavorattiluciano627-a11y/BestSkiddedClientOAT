package dev.echo.client.utils.placement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Wall-first center-column enclosure search with the original support-column tie-breaker. */
public class NearestPlacementSearch extends PlacementSearch {
    public NearestPlacementSearch() {
    }

    @Override
    public boolean allowsPartialSupport() {
        return true;
    }

    @Override
    protected Predicate<ItemStack> stackFilter() {
        return PlacementStackPredicates.WALL_ITEMS;
    }

    @Override
    protected boolean includeRaisedPathPosition() {
        return false;
    }

    @Override
    public List<PlacementFilter> plan(LivingEntity target, Vec3 displacement) {
        PlacementBounds bounds = PlacementBounds.atCenter(target, displacement, PlacementWorldQueries::isReplaceable);
        Vec3 eye = CLIENT.player.getEyePosition();
        Comparator<BlockPos> farthest = PlacementDistances.farthestFirst(eye);
        List<BlockPos> top = roof(bounds);
        List<BlockPos> column = top.isEmpty() || anyPlaceable(top) ? List.of() : supportColumn(bounds, eye);
        List<BlockPos> sides = perimeterAt(bounds, bounds.minY());
        sides.removeAll(column);
        sides.sort(farthest);
        top.sort(farthest);
        List<BlockPos> below = sides.stream().map(BlockPos::below).filter(PlacementWorldQueries::isReplaceable).toList();
        List<PlacementFilter> result = new ArrayList<>();
        append(result, sides, PlacementStackPredicates.WALL_ITEMS);
        append(result, below, PlacementStackPredicates.WALL_ITEMS);
        append(result, column, PlacementStackPredicates.WALL_ITEMS);
        append(result, column, PlacementStackPredicates.FULL_CUBE_ITEMS);
        append(result, top, PlacementStackPredicates.WALL_ITEMS);
        append(result, top, PlacementStackPredicates.FULL_CUBE_ITEMS);
        return result;
    }

    private static List<BlockPos> supportColumn(PlacementBounds bounds, Vec3 eye) {
        return perimeter(bounds).stream()
                .map(base -> IntStream.rangeClosed(bounds.minY(), bounds.aboveTop()).mapToObj(base::atY)
                        .filter(PlacementWorldQueries::isReplaceable).toList())
                .filter(column -> !column.isEmpty())
                .filter(column -> column.stream().noneMatch(PlacementWorldQueries::hasBlockingEntity))
                .min(Comparator.<List<BlockPos>>comparingInt(List::size)
                        .thenComparingDouble(column -> PlacementDistances.farthestColumnScore(eye, column)))
                .orElse(List.of());
    }
}
