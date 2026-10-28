package dev.echo.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.echo.client.Echo;
import dev.echo.client.events.movement.RotationContext;
import dev.echo.client.events.movement.RotationEvent;
import dev.echo.client.features.impl.render.SwingSpeedFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
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

    public LivingEntityRotationMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;applyInput()V", shift = At.Shift.AFTER))
    private void applyMovementRotation(CallbackInfo callback) {
        if (!Echo.eventsDisabled && (Object) this == Minecraft.getInstance().player) {
            RotationEvent rotation = RotationEvent.dispatch(RotationContext.MOVEMENT, getYRot(), getXRot());
            echo$rotationOverridden = rotation.getYaw() != getYRot() || rotation.getPitch() != getXRot();
            if (echo$rotationOverridden) {
                echo$savedYaw = getYRot();
                echo$savedPitch = getXRot();
                setYRot(rotation.getYaw());
                setXRot(rotation.getPitch());
            }
        }
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void restoreMovementRotation(CallbackInfo callback) {
        if (echo$rotationOverridden) {
            echo$rotationOverridden = false;
            setYRot(echo$savedYaw);
            setXRot(echo$savedPitch);
        }
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F", ordinal = 0))
    private float modifyFirstTickYaw(float original) {
        return echo$resolveBodyYaw(original);
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F", ordinal = 1))
    private float modifySecondTickYaw(float original) {
        return echo$resolveBodyYaw(original);
    }

    @ModifyExpressionValue(method = "tickHeadTurn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float modifyHeadTurnYaw(float original) {
        return echo$resolveBodyYaw(original);
    }

    @Unique
    private float echo$resolveBodyYaw(float original) {
        return (Object) this != Minecraft.getInstance().player ? original
                : RotationEvent.dispatch(RotationContext.BODY, original,
                        Minecraft.getInstance().player.getXRot()).getYaw();
    }

    @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
    private void modifyHandSwingDuration(CallbackInfoReturnable<Integer> callback) {
        if (!Echo.eventsDisabled && Echo.featureManager != null && (Object) this == Minecraft.getInstance().player) {
            SwingSpeedFeature swing = (SwingSpeedFeature) Echo.featureManager.findFeature(SwingSpeedFeature.class);
            if (swing != null && swing.isEnabled()) {
                callback.setReturnValue(SwingSpeedFeature.swingSpeed.getValue());
            }
        }
    }
}
