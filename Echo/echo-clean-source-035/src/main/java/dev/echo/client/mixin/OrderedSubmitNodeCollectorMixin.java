package dev.echo.client.mixin;

import dev.echo.render.HandChamsRenderer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The {@code submitModelPart} overloads are default methods on the collector
 * interface, so this one hook needs an interface mixin (handlers must be
 * {@code default}). It is quarantined: the trailing int argument moved in 26.2
 * and a wrong ordinal must degrade to a warning, not abort the class transform.
 */
@Mixin(OrderedSubmitNodeCollector.class)
public interface OrderedSubmitNodeCollectorMixin {
    @ModifyVariable(method = "submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;I)V",
            at = @At("HEAD"), ordinal = 3, argsOnly = true, require = 0)
    default int echo$modifySubmitModelPart(int outlineColor) {
        return HandChamsRenderer.overrideOutlineColor(outlineColor);
    }
}
