package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.render.PerspectiveEvent;
import net.minecraft.client.CameraType;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets perspective subscribers veto a pending perspective change. */
@Mixin(Options.class)
public class GameOptionsMixin {
    @Inject(method = "setCameraType", at = @At("HEAD"), cancellable = true)
    private void onSetPerspectiveHead(CameraType perspective, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            PerspectiveEvent event = new PerspectiveEvent(perspective);
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }
}
