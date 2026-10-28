package dev.echo.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.echo.client.Echo;
import dev.echo.client.features.impl.render.FullBrightFeature;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {
    @Shadow @Final private GpuTexture glTexture;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void replaceLightmapWithWhite(float tickProgress, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (FullBrightFeature.isFullBrightActive()) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(glTexture, -1);
            callback.cancel();
        }
    }
}
