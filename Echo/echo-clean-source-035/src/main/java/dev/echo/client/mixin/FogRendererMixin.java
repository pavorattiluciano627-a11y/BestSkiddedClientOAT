package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.features.impl.render.AmbienceFeature;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Ambience fog colour.
 *
 * In 26.2 {@code setupFog} returns {@code FogData} instead of a {@code Vector4f},
 * and the old internal {@code applyFog(ByteBuffer, int, Vector4f, ...)} call is
 * gone -- the colour is written with {@code Vector4f.set(r, g, b, a)} at the end
 * of {@code computeFogColor}. So the hook sits on that call and rewrites the
 * three colour channels.
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @ModifyArgs(method = "computeFogColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IFLorg/joml/Vector4f;)V",
            at = @At(value = "INVOKE",
                    target = "Lorg/joml/Vector4f;set(FFFF)Lorg/joml/Vector4f;"),
            require = 0)
    private void echo$modifyFogColor(Args arguments) {
        if (Echo.eventsDisabled || MinecraftAccess.CLIENT.player == null
                || MinecraftAccess.CLIENT.level == null || Echo.featureManager == null) {
            return;
        }
        AmbienceFeature ambience = (AmbienceFeature) Echo.featureManager.findFeature(AmbienceFeature.class);
        if (ambience != null && ambience.isEnabled() && AmbienceFeature.isFogColorEnabled()) {
            arguments.set(0, AmbienceFeature.getFogRed() / 255.0F);
            arguments.set(1, AmbienceFeature.getFogGreen() / 255.0F);
            arguments.set(2, AmbienceFeature.getFogBlue() / 255.0F);
        }
    }
}
