package dev.echo.client.mixin;

import dev.echo.client.render.PlayerOverlayFeature;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("TAIL"))
    private void addPlayerOverlay(CallbackInfo callback) {
        ((LivingEntityRendererAccessor) this).invokeAddFeature(new PlayerOverlayFeature(
                (FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>) (Object) this));
    }
}
