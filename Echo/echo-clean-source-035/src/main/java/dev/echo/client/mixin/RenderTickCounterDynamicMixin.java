package dev.echo.client.mixin;

import dev.echo.client.bridge.TickSpeedControl;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DeltaTracker.Timer.class)
public class RenderTickCounterDynamicMixin implements TickSpeedControl {
    @Shadow private float deltaTicks;
    @Unique private float echo$tickSpeedMultiplier = 1.0F;

    @Inject(method = "advanceGameTime(J)I", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/DeltaTracker$Timer;lastMs:J", ordinal = 1), require = 0)
    private void scaleFrameDuration(long timeMillis, CallbackInfoReturnable<Integer> callback) {
        deltaTicks *= getTickSpeedMultiplier();
    }

    @Override
    public float getTickSpeedMultiplier() {
        return echo$tickSpeedMultiplier;
    }

    @Override
    public void setTickSpeedMultiplier(float multiplier) {
        echo$tickSpeedMultiplier = multiplier;
    }
}
