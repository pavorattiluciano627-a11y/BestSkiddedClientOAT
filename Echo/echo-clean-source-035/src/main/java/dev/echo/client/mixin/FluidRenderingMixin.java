package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.EventBus;
import dev.echo.client.events.render.FluidRenderEvent;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cancels Fabric fluid rendering when a subscribed fluid-render event is cancelled. */
@Mixin(value = FluidRendering.class, remap = false)
public class FluidRenderingMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private static void onRenderHead(CallbackInfo callback) {
        if (!Echo.eventsDisabled && EventBus.hasSubscribers(FluidRenderEvent.class)) {
            if (new FluidRenderEvent().post()) {
                callback.cancel();
            }
        }
    }
}
