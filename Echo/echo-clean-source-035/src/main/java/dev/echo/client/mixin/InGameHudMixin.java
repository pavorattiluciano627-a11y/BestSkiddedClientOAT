package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.render.InGameHudEvent;
import dev.echo.client.events.render.NauseaOverlayEvent;
import dev.echo.client.events.render.PortalOverlayEvent;
import dev.echo.client.events.render.SpyglassOverlayEvent;
import dev.echo.client.events.render.VignetteOverlayEvent;
import dev.echo.client.features.impl.render.HotbarFeature;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            InGameHudEvent event = new InGameHudEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    public void onRenderHotbarHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo callback) {
        if (!Echo.eventsDisabled && HotbarFeature.replacesVanillaHotbar()) {
            callback.cancel();
        }
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    public void onRenderVignetteOverlayHead(DrawContext context, Entity entity, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            VignetteOverlayEvent event = new VignetteOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true)
    public void onRenderNauseaOverlayHead(DrawContext context, float strength, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            NauseaOverlayEvent event = new NauseaOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    public void onRenderSpyglassOverlayHead(DrawContext context, float scale, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            SpyglassOverlayEvent event = new SpyglassOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    public void onRenderPortalOverlayHead(DrawContext context, float strength, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            PortalOverlayEvent event = new PortalOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }
}
