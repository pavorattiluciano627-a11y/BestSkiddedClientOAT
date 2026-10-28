package dev.echo.client.feature.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

/** A living target and the intersection of its expanded box with the pick ray.
 * @param entity target entity
 * @param hitPosition ray intersection
 */
public record PiercingTarget(LivingEntity entity, Vec3d hitPosition) {
    /** Converts the stored intersection to Minecraft's ordinary hit result.
     * @return hit result retaining the same entity and position
     */
    public EntityHitResult toHitResult() {
        return new EntityHitResult(entity, hitPosition);
    }
}
