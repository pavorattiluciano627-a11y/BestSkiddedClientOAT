package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.render.InGameHudEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class InGameHudMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void onRenderHead(DeltaTracker tickCounter, boolean renderHud, boolean renderOverlay,
            CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            InGameHudEvent event = new InGameHudEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }
}
