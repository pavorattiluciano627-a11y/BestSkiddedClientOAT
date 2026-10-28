package dev.echo.client.utils.placement;

import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Distance operations shared by placement searches. */
public final class PlacementDistances {
    private PlacementDistances() {
    }

    public static Comparator<BlockPos> farthestFirst(Vec3 origin) {
        return Comparator.<BlockPos>comparingDouble(position -> position.distToCenterSqr(origin)).reversed();
    }

    public static double horizontalSquaredDistance(BlockPos position, Vec3 origin) {
        return Mth.square(position.getX() + 0.5 - origin.x)
                + Mth.square(position.getZ() + 0.5 - origin.z);
    }

    public static double farthestColumnScore(Vec3 origin, List<BlockPos> column) {
        return -column.get(0).distToCenterSqr(origin);
    }
}
