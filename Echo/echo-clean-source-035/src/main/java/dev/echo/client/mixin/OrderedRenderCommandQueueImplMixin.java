package dev.echo.client.mixin;

import dev.echo.render.HandChamsRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Outline-colour override for submitted model/item draws.
 *
 * 26.2 split the render-queue API in two: {@code OrderedSubmitNodeCollector} is
 * the interface, {@code SubmitNodeStorage} the concrete collector. These two
 * overloads are declared on the class, so they are hooked here — a class mixin
 * cannot target an interface (Mixin aborts the config at prepare time).
 */
@Mixin(SubmitNodeStorage.class)
public class OrderedRenderCommandQueueImplMixin {
    @ModifyVariable(method = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At("HEAD"), ordinal = 3, argsOnly = true)
    private int modifySubmitModel(int outlineColor) {
        return HandChamsRenderer.overrideOutlineColor(outlineColor);
    }

    @ModifyVariable(method = "submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V",
            at = @At("HEAD"), ordinal = 2, argsOnly = true)
    private int modifySubmitItem(int outlineColor) {
        return HandChamsRenderer.overrideOutlineColor(outlineColor);
    }
}
