package dev.echo.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.echo.client.Echo;
import dev.echo.client.features.impl.render.NoRenderFeature;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndCrystalRenderer.class)
public abstract class EndCrystalEntityRendererMixin {
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("HEAD"))
    public void onRenderHead(EndCrystalRenderState state, PoseStack matrices,
                              SubmitNodeCollector commands, CameraRenderState camera, CallbackInfo callback) {
        if (!Echo.eventsDisabled && Echo.featureManager != null) {
            NoRenderFeature noRender = (NoRenderFeature) Echo.featureManager.findFeature(NoRenderFeature.class);
            if (noRender != null && noRender.isEnabled() && NoRenderFeature.noCrystalBounce.getValue()) {
                state.ageInTicks = 0.0F;
            }
        }
    }
}
