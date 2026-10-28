package dev.echo.client.utils.trajectory;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/** Shared owner-departure predicate used by all five projectile simulations. */
public final class ProjectileOwnerBounds {
    private ProjectileOwnerBounds() {
    }

    public static boolean intersects(AABB sweptBounds, Entity entity) {
        return sweptBounds.intersects(entity.getBoundingBox());
    }
}
