package dev.echo.client.utils.placement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Layered enclosure search retaining the caller's exact stack predicate. */
public class FilteredPlacementSearch extends PlacementSearch {
    private final Predicate<ItemStack> filter;

    public FilteredPlacementSearch(Predicate<ItemStack> filter) {
        this.filter = filter;
    }

    @Override
    protected Predicate<ItemStack> stackFilter() {
        return filter;
    }

    @Override
    public List<PlacementFilter> plan(LivingEntity target, Vec3 displacement) {
        PlacementBounds bounds = PlacementBounds.around(target, displacement, PlacementWorldQueries::isReplaceable);
        Vec3 eye = CLIENT.player.getEyePosition();
        Comparator<BlockPos> farthest = PlacementDistances.farthestFirst(eye);
        List<PlacementFilter> result = new ArrayList<>();
        for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
            List<BlockPos> layer = perimeterAt(bounds, y);
            layer.sort(farthest);
            append(result, layer, filter);
        }
        List<BlockPos> top = roof(bounds);
        if (!top.isEmpty() && !anyPlaceable(top)) {
            List<BlockPos> roofSupport = perimeterAt(bounds, bounds.aboveTop());
            roofSupport.sort(Comparator.<BlockPos>comparingInt(position -> hasAdjacentVisibleFace(position, top, eye) ? 0 : 1)
                    .thenComparing(farthest));
            append(result, roofSupport, filter);
        }
        top.sort(farthest);
        append(result, top, filter);
        return result;
    }
}
