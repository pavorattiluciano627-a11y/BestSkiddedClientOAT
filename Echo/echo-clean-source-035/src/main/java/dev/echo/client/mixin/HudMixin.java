package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.render.NauseaOverlayEvent;
import dev.echo.client.events.render.PortalOverlayEvent;
import dev.echo.client.events.render.SpyglassOverlayEvent;
import dev.echo.client.events.render.VignetteOverlayEvent;
import dev.echo.client.features.impl.render.HotbarFeature;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.2 moved the individual HUD extractors off {@code Gui} onto {@code Hud}.
 * The cancellation hooks follow them there.
 */
@Mixin(Hud.class)
public abstract class HudMixin {
    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    public void onRenderHotbarHead(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo callback) {
        if (!Echo.eventsDisabled && HotbarFeature.replacesVanillaHotbar()) {
            callback.cancel();
        }
    }

    @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
    public void onRenderVignetteOverlayHead(GuiGraphicsExtractor context, Entity entity, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            VignetteOverlayEvent event = new VignetteOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
    public void onRenderNauseaOverlayHead(GuiGraphicsExtractor context, float strength, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            NauseaOverlayEvent event = new NauseaOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "extractSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    public void onRenderSpyglassOverlayHead(GuiGraphicsExtractor context, float scale, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            SpyglassOverlayEvent event = new SpyglassOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "extractPortalOverlay", at = @At("HEAD"), cancellable = true)
    public void onRenderPortalOverlayHead(GuiGraphicsExtractor context, float strength, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            PortalOverlayEvent event = new PortalOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }
}
