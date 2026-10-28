package dev.echo.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.movement.RotationContext;
import dev.echo.client.events.movement.RotationEvent;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRenderRotationMixin {
    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"))
    private float modifyUpdateRenderStateAtGetLerpedPitch(float pitch, LivingEntity entity,
                                                         LivingEntityRenderState state, float tickProgress) {
        if (entity != MinecraftAccess.CLIENT.player || MinecraftAccess.CLIENT.options.getPerspective().isFirstPerson()) {
            return pitch;
        }
        return RotationEvent.dispatch(RotationContext.RENDER_PITCH, entity.getYaw(), pitch, tickProgress).getPitch();
    }
}
