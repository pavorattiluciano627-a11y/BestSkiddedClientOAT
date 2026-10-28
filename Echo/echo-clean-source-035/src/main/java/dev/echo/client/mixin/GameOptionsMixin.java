package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.render.PerspectiveEvent;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets perspective subscribers veto a pending perspective change. */
@Mixin(GameOptions.class)
public class GameOptionsMixin {
    @Inject(method = "setPerspective", at = @At("HEAD"), cancellable = true)
    private void onSetPerspectiveHead(Perspective perspective, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            PerspectiveEvent event = new PerspectiveEvent(perspective);
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }
}
