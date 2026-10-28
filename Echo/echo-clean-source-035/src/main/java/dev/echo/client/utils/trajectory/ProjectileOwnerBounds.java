package dev.echo.client.utils.trajectory;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

/** Shared owner-departure predicate used by all five projectile simulations. */
public final class ProjectileOwnerBounds {
    private ProjectileOwnerBounds() {
    }

    public static boolean intersects(Box sweptBounds, Entity entity) {
        return sweptBounds.intersects(entity.getBoundingBox());
    }
}
