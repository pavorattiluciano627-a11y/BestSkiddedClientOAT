package dev.echo.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.movement.RotationContext;
import dev.echo.client.events.movement.RotationEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin extends Entity {
    public FireworkRocketEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d wrapTickAtGetRotationVector(LivingEntity entity, Operation<Vec3d> original) {
        if (entity != MinecraftAccess.CLIENT.player) {
            return original.call(entity);
        }
        RotationEvent rotation = RotationEvent.dispatch(RotationContext.MOVEMENT, entity.getYaw(), entity.getPitch());
        return getRotationVector(rotation.getPitch(), rotation.getYaw());
    }
}
