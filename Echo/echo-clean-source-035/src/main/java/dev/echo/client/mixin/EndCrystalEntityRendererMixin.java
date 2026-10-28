package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.features.impl.render.NoRenderFeature;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndCrystalEntityRenderer.class)
public abstract class EndCrystalEntityRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
        at = @At("HEAD"))
    public void onRenderHead(EndCrystalEntityRenderState state, MatrixStack matrices,
                              OrderedRenderCommandQueue commands, CameraRenderState camera, CallbackInfo callback) {
        if (!Echo.eventsDisabled && Echo.featureManager != null) {
            NoRenderFeature noRender = (NoRenderFeature) Echo.featureManager.findFeature(NoRenderFeature.class);
            if (noRender != null && noRender.isEnabled() && NoRenderFeature.noCrystalBounce.getValue()) {
                state.age = 0.0F;
            }
        }
    }
}
