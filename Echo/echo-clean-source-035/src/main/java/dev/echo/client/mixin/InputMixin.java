package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.features.impl.movement.SprintFeature;
import net.minecraft.client.input.Input;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Input.class)
public class InputMixin {
    @Shadow protected Vec2f movementVector;

    @Inject(method = "hasForwardMovement", at = @At("HEAD"), cancellable = true)
    private void allowOmnidirectionalSprint(CallbackInfoReturnable<Boolean> callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (Echo.featureManager != null) {
            SprintFeature sprint = (SprintFeature) Echo.featureManager.findFeature(SprintFeature.class);
            if (sprint != null && sprint.isOmniEnabled()) {
                callback.setReturnValue(movementVector.lengthSquared() > 1.0E-5F);
            }
        }
    }
}
