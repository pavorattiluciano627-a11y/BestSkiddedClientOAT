package dev.echo.client.mixin;

import dev.echo.client.render.PlayerOverlayFeature;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class PlayerEntityRendererMixin {
    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("TAIL"))
    private void addPlayerOverlay(CallbackInfo callback) {
        ((LivingEntityRendererAccessor) this).invokeAddFeature(new PlayerOverlayFeature(
                (RenderLayerParent<AvatarRenderState, PlayerModel>) (Object) this));
    }
}
