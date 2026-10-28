package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.world.EntityHitboxEvent;
import dev.echo.client.features.impl.utility.AntiTrapFeature;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Candidate: AntiTrap key removal and event getter contract require callee proof. */
@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {
    @Redirect(method = "raycast", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getBoundingBox()Lnet/minecraft/util/math/Box;"))
    private static Box redirectRaycastAtGetBoundingBox(Entity entity) {
        Box targetingBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
        if (Echo.eventsDisabled) {
            return targetingBox;
        }
        if (AntiTrapFeature.shouldIgnoreEntity(entity)) {
            double excludedY = entity.getY() - 1024.0;
            return new Box(entity.getX(), excludedY, entity.getZ(), entity.getX(), excludedY, entity.getZ());
        }
        EntityHitboxEvent event = new EntityHitboxEvent(entity, targetingBox);
        event.post();
        return event.getBoundingBox();
    }
}
