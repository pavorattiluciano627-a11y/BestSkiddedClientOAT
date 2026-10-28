package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.features.impl.render.AmbienceFeature;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Candidate: Ambience color-channel getters and key-free lookup require callee proof. */
@Mixin(SkyRenderer.class)
public class SkyRenderingColorMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onUpdateRenderStateTail(ClientLevel world, float tickProgress, Camera camera,
            SkyRenderState state, CallbackInfo callback) {
        if (!Echo.eventsDisabled && MinecraftAccess.CLIENT.player != null && MinecraftAccess.CLIENT.level != null
                && Echo.featureManager != null) {
            AmbienceFeature ambience = (AmbienceFeature) Echo.featureManager.findFeature(AmbienceFeature.class);
            if (ambience != null && ambience.isEnabled() && AmbienceFeature.isFogColorEnabled()) {
                state.skyColor = ARGB.color(AmbienceFeature.getFogRed(),
                        AmbienceFeature.getFogGreen(), AmbienceFeature.getFogBlue());
                if (AmbienceFeature.isGradientEnabled()) {
                    state.sunriseAndSunsetColor = ARGB.color(255, AmbienceFeature.getGradientRed(),
                            AmbienceFeature.getGradientGreen(), AmbienceFeature.getGradientBlue());
                } else {
                    state.sunriseAndSunsetColor = 0;
                }
                state.shouldRenderDarkDisc = false;
            }
        }
    }
}
