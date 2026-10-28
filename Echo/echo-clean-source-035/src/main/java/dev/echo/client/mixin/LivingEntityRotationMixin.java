package dev.echo.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.echo.client.Echo;
import dev.echo.client.events.movement.RotationContext;
import dev.echo.client.events.movement.RotationEvent;
import dev.echo.client.features.impl.render.SwingSpeedFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRotationMixin extends Entity {
    @Unique private boolean echo$rotationOverridden;
    @Unique private float echo$savedYaw;
    @Unique private float echo$savedPitch;

    public LivingEntityRotationMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "tickMovement", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;tickMovementInput()V", shift = At.Shift.AFTER))
    private void applyMovementRotation(CallbackInfo callback) {
        if (!Echo.eventsDisabled && (Object) this == MinecraftClient.getInstance().player) {
            RotationEvent rotation = RotationEvent.dispatch(RotationContext.MOVEMENT, getYaw(), getPitch());
            echo$rotationOverridden = rotation.getYaw() != getYaw() || rotation.getPitch() != getPitch();
            if (echo$rotationOverridden) {
                echo$savedYaw = getYaw();
                echo$savedPitch = getPitch();
                setYaw(rotation.getYaw());
                setPitch(rotation.getPitch());
            }
        }
    }

    @Inject(method = "tickMovement", at = @At("RETURN"))
    private void restoreMovementRotation(CallbackInfo callback) {
        if (echo$rotationOverridden) {
            echo$rotationOverridden = false;
            setYaw(echo$savedYaw);
            setPitch(echo$savedPitch);
        }
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;getYaw()F", ordinal = 0))
    private float modifyFirstTickYaw(float original) {
        return echo$resolveBodyYaw(original);
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;getYaw()F", ordinal = 1))
    private float modifySecondTickYaw(float original) {
        return echo$resolveBodyYaw(original);
    }

    @ModifyExpressionValue(method = "turnHead", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    private float modifyHeadTurnYaw(float original) {
        return echo$resolveBodyYaw(original);
    }

    @Unique
    private float echo$resolveBodyYaw(float original) {
        return (Object) this != MinecraftClient.getInstance().player ? original
                : RotationEvent.dispatch(RotationContext.BODY, original,
                        MinecraftClient.getInstance().player.getPitch()).getYaw();
    }

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void modifyHandSwingDuration(CallbackInfoReturnable<Integer> callback) {
        if (!Echo.eventsDisabled && Echo.featureManager != null && (Object) this == MinecraftClient.getInstance().player) {
            SwingSpeedFeature swing = (SwingSpeedFeature) Echo.featureManager.findFeature(SwingSpeedFeature.class);
            if (swing != null && swing.isEnabled()) {
                callback.setReturnValue(SwingSpeedFeature.swingSpeed.getValue());
            }
        }
    }
}
