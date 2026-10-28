package dev.echo.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.movement.RotationContext;
import dev.echo.client.events.movement.RotationEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin extends Entity {
    public FireworkRocketEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 wrapTickAtGetRotationVector(LivingEntity entity, Operation<Vec3> original) {
        if (entity != MinecraftAccess.CLIENT.player) {
            return original.call(entity);
        }
        RotationEvent rotation = RotationEvent.dispatch(RotationContext.MOVEMENT, entity.getYRot(), entity.getXRot());
        return calculateViewVector(rotation.getPitch(), rotation.getYaw());
    }
}
