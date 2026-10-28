package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.world.EntityHitboxEvent;
import dev.echo.client.features.impl.utility.AntiTrapFeature;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Candidate: AntiTrap key removal and event getter contract require callee proof. */
@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {
    @Redirect(method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"), require = 0)
    private static AABB redirectRaycastAtGetBoundingBox(Entity entity) {
        AABB targetingBox = entity.getBoundingBox().inflate(entity.getPickRadius());
        if (Echo.eventsDisabled) {
            return targetingBox;
        }
        if (AntiTrapFeature.shouldIgnoreEntity(entity)) {
            double excludedY = entity.getY() - 1024.0;
            return new AABB(entity.getX(), excludedY, entity.getZ(), entity.getX(), excludedY, entity.getZ());
        }
        EntityHitboxEvent event = new EntityHitboxEvent(entity, targetingBox);
        event.post();
        return event.getBoundingBox();
    }
}
