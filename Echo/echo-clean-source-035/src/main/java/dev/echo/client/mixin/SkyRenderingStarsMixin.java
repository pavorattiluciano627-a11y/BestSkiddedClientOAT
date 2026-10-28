package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.features.impl.render.SkyShadersFeature;
import dev.echo.render.FullscreenEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public class SkyRenderingStarsMixin {
    @Unique
    private static boolean echo$hasCustomSky() {
        if (Echo.eventsDisabled) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || Echo.featureManager == null) {
            return false;
        }
        SkyShadersFeature sky = (SkyShadersFeature) Echo.featureManager.findFeature(SkyShadersFeature.class);
        return sky != null && sky.isEnabled();
    }

    @Inject(method = "renderSkyDisc", at = @At("HEAD"), cancellable = true)
    private void replaceTopSky(int color, CallbackInfo callback) {
        if (echo$hasCustomSky() && FullscreenEffects.renderCustomSky()) {
            callback.cancel();
        }
    }

    @Inject(method = "renderEndSky", at = @At("HEAD"), cancellable = true)
    private void replaceEndSky(CallbackInfo callback) {
        if (echo$hasCustomSky() && SkyShadersFeature.shouldRenderInEnd() && FullscreenEffects.renderCustomSky()) {
            callback.cancel();
        }
    }

    @Inject(method = "renderSunriseAndSunset", at = @At("HEAD"), cancellable = true)
    private void suppressGlowingSky(CallbackInfo callback) {
        if (echo$hasCustomSky()) {
            callback.cancel();
        }
    }

    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"), cancellable = true)
    private void controlCelestialBodies(CallbackInfo callback) {
        if (echo$hasCustomSky() && !SkyShadersFeature.shouldRenderSunAndMoon()) {
            callback.cancel();
        }
    }

    @Inject(method = "renderEndFlash", at = @At("HEAD"), cancellable = true)
    private void suppressEndLightFlash(CallbackInfo callback) {
        if (echo$hasCustomSky() && SkyShadersFeature.shouldRenderInEnd()) {
            callback.cancel();
        }
    }
}
