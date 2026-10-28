package dev.echo.client.feature.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** A living target and the intersection of its expanded box with the pick ray.
 * @param entity target entity
 * @param hitPosition ray intersection
 */
public record PiercingTarget(LivingEntity entity, Vec3 hitPosition) {
    /** Converts the stored intersection to Minecraft's ordinary hit result.
     * @return hit result retaining the same entity and position
     */
    public EntityHitResult toHitResult() {
        return new EntityHitResult(entity, hitPosition);
    }
}
