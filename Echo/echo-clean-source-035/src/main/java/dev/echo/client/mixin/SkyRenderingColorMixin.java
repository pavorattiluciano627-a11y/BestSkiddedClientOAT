package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.features.impl.render.AmbienceFeature;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Candidate: Ambience color-channel getters and key-free lookup require callee proof. */
@Mixin(SkyRendering.class)
public class SkyRenderingColorMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void onUpdateRenderStateTail(ClientWorld world, float tickProgress, Camera camera,
            SkyRenderState state, CallbackInfo callback) {
        if (!Echo.eventsDisabled && MinecraftAccess.CLIENT.player != null && MinecraftAccess.CLIENT.world != null
                && Echo.featureManager != null) {
            AmbienceFeature ambience = (AmbienceFeature) Echo.featureManager.findFeature(AmbienceFeature.class);
            if (ambience != null && ambience.isEnabled() && AmbienceFeature.isFogColorEnabled()) {
                state.skyColor = ColorHelper.getArgb(AmbienceFeature.getFogRed(),
                        AmbienceFeature.getFogGreen(), AmbienceFeature.getFogBlue());
                if (AmbienceFeature.isGradientEnabled()) {
                    state.sunriseAndSunsetColor = ColorHelper.getArgb(255, AmbienceFeature.getGradientRed(),
                            AmbienceFeature.getGradientGreen(), AmbienceFeature.getGradientBlue());
                } else {
                    state.sunriseAndSunsetColor = 0;
                }
                state.shouldRenderSkyDark = false;
            }
        }
    }
}
