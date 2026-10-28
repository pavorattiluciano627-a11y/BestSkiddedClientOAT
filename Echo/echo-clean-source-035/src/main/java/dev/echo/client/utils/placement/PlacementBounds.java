package dev.echo.client.utils.placement;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Candidate bounds logic. The predicate argument exposes the still-unresolved
 * block test at the original support search callsite; its target is not guessed.
 */
public record PlacementBounds(int minX, int maxX, int minZ, int maxZ, int minY, int maxY) {
    public static PlacementBounds around(LivingEntity entity, Vec3 offset,
            Predicate<BlockPos> replaceable) {
        AABB box = entity.getBoundingBox().move(offset).deflate(1.0E-4);
        int minX = Mth.floor(box.minX);
        int maxX = Mth.floor(box.maxX);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.floor(box.maxZ);
        int minY = supportLevel(minX, maxX, minZ, maxZ, Mth.floor(box.minY), replaceable);
        return new PlacementBounds(minX, maxX, minZ, maxZ, minY,
                Mth.floor(minY + box.getYsize()));
    }

    public static PlacementBounds atCenter(LivingEntity entity, Vec3 offset,
            Predicate<BlockPos> replaceable) {
        AABB box = entity.getBoundingBox().move(offset).deflate(1.0E-4);
        BlockPos center = BlockPos.containing(entity.position().add(offset));
        int minY = supportLevel(center.getX(), center.getX(), center.getZ(), center.getZ(),
                center.getY(), replaceable);
        return new PlacementBounds(center.getX(), center.getX(), center.getZ(), center.getZ(),
                minY, Mth.floor(minY + box.getYsize()));
    }

    private static int supportLevel(int minX, int maxX, int minZ, int maxZ, int startingY,
            Predicate<BlockPos> replaceable) {
        for (int y = startingY; y > startingY - 4; y--) {
            for (BlockPos below : BlockPos.betweenClosed(minX, y - 1, minZ, maxX, y - 1, maxZ)) {
                if (!replaceable.test(below)) {
                    return y;
                }
            }
        }
        // The original returns startingY, rather than the exhausted search depth.
        return startingY;
    }

    public int aboveTop() {
        return maxY + 1;
    }
}
