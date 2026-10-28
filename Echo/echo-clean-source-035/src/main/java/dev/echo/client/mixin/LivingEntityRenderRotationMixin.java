package dev.echo.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.movement.RotationContext;
import dev.echo.client.events.movement.RotationEvent;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRenderRotationMixin {
    @ModifyExpressionValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot(F)F"))
    private float modifyUpdateRenderStateAtGetLerpedPitch(float pitch, LivingEntity entity,
                                                         LivingEntityRenderState state, float tickProgress) {
        if (entity != MinecraftAccess.CLIENT.player || MinecraftAccess.CLIENT.options.getCameraType().isFirstPerson()) {
            return pitch;
        }
        return RotationEvent.dispatch(RotationContext.RENDER_PITCH, entity.getYRot(), pitch, tickProgress).getPitch();
    }
}
