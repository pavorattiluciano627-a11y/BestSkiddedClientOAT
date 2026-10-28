package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.features.impl.movement.SprintFeature;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientInput.class)
public class InputMixin {
    @Shadow protected Vec2 moveVector;

    @Inject(method = "hasForwardImpulse", at = @At("HEAD"), cancellable = true)
    private void allowOmnidirectionalSprint(CallbackInfoReturnable<Boolean> callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (Echo.featureManager != null) {
            SprintFeature sprint = (SprintFeature) Echo.featureManager.findFeature(SprintFeature.class);
            if (sprint != null && sprint.isOmniEnabled()) {
                callback.setReturnValue(moveVector.lengthSquared() > 1.0E-5F);
            }
        }
    }
}
