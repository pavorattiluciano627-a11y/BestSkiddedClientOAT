package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.features.impl.render.AmbienceFeature;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Inject(method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
        at = @At("RETURN"), cancellable = true)
    private void onApplyFogReturn(Camera camera, int viewDistance, RenderTickCounter tickCounter,
                                  float skyDarkness, ClientWorld world, CallbackInfoReturnable<Vector4f> callback) {
        if (!Echo.eventsDisabled && MinecraftAccess.CLIENT.player != null
                && MinecraftAccess.CLIENT.world != null && Echo.featureManager != null) {
            AmbienceFeature ambience = (AmbienceFeature) Echo.featureManager.findFeature(AmbienceFeature.class);
            if (ambience != null && ambience.isEnabled() && AmbienceFeature.isFogColorEnabled()) {
                float red = AmbienceFeature.getFogRed() / 255.0F;
                float green = AmbienceFeature.getFogGreen() / 255.0F;
                float blue = AmbienceFeature.getFogBlue() / 255.0F;
                callback.setReturnValue(new Vector4f(red, green, blue, 1.0F));
            }
        }
    }

    @ModifyArgs(method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
    private void modifyArgumentsOfApplyFogAtApplyFog(Args arguments) {
        if (!Echo.eventsDisabled && MinecraftAccess.CLIENT.player != null
                && MinecraftAccess.CLIENT.world != null && Echo.featureManager != null) {
            AmbienceFeature ambience = (AmbienceFeature) Echo.featureManager.findFeature(AmbienceFeature.class);
            if (ambience != null && ambience.isEnabled() && AmbienceFeature.isFogColorEnabled()) {
                float red = AmbienceFeature.getFogRed() / 255.0F;
                float green = AmbienceFeature.getFogGreen() / 255.0F;
                float blue = AmbienceFeature.getFogBlue() / 255.0F;
                float distance = AmbienceFeature.getFogDistanceChunks() * 16.0F;
                arguments.set(2, new Vector4f(red, green, blue, 1.0F));
                arguments.set(4, distance);
                arguments.set(7, distance);
                arguments.set(8, distance);
            }
        }
    }
}
