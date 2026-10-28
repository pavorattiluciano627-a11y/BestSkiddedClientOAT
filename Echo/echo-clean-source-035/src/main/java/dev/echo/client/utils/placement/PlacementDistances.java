package dev.echo.client.utils.placement;

import java.util.Comparator;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Distance operations shared by placement searches. */
public final class PlacementDistances {
    private PlacementDistances() {
    }

    public static Comparator<BlockPos> farthestFirst(Vec3d origin) {
        return Comparator.<BlockPos>comparingDouble(position -> position.getSquaredDistance(origin)).reversed();
    }

    public static double horizontalSquaredDistance(BlockPos position, Vec3d origin) {
        return MathHelper.square(position.getX() + 0.5 - origin.x)
                + MathHelper.square(position.getZ() + 0.5 - origin.z);
    }

    public static double farthestColumnScore(Vec3d origin, List<BlockPos> column) {
        return -column.get(0).getSquaredDistance(origin);
    }
}
