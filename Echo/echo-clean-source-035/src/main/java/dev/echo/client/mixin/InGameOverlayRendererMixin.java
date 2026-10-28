package dev.echo.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.echo.client.Echo;
import dev.echo.client.events.render.FireOverlayEvent;
import dev.echo.client.events.render.FloatingItemOverlayEvent;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserves independent fire and floating-item overlay cancellation events. */
@Mixin(ScreenEffectRenderer.class)
public class InGameOverlayRendererMixin {
    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void onRenderFireOverlayHead(PoseStack matrices, SubmitNodeCollector queue,
            TextureAtlasSprite fireSprite, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            FireOverlayEvent event = new FireOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), cancellable = true)
    public void onRenderFloatingItemHead(PoseStack matrices, float tickProgress,
            SubmitNodeCollector queue, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            FloatingItemOverlayEvent event = new FloatingItemOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }
}
