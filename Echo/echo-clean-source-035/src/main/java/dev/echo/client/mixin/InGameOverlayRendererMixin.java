package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.render.FireOverlayEvent;
import dev.echo.client.events.render.FloatingItemOverlayEvent;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserves independent fire and floating-item overlay cancellation events. */
@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void onRenderFireOverlayHead(MatrixStack matrices, VertexConsumerProvider vertices,
            Sprite fireSprite, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            FireOverlayEvent event = new FireOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "renderFloatingItem", at = @At("HEAD"), cancellable = true)
    public void onRenderFloatingItemHead(MatrixStack matrices, float tickProgress,
            OrderedRenderCommandQueue queue, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            FloatingItemOverlayEvent event = new FloatingItemOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }
}
