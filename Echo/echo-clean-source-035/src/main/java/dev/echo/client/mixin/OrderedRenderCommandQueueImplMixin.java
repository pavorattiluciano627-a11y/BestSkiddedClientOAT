package dev.echo.client.mixin;

import dev.echo.render.HandChamsRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Candidate: key-free outline-color transform requires HandChamsRenderer callee proof. */
@Mixin(SubmitNodeStorage.class)
public class OrderedRenderCommandQueueImplMixin {
    @ModifyVariable(method = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At("HEAD"), ordinal = 3, argsOnly = true)
    private int modifySubmitModel(int outlineColor) {
        return HandChamsRenderer.overrideOutlineColor(outlineColor);
    }

    @ModifyVariable(method = "submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ZZILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;I)V",
            at = @At("HEAD"), ordinal = 3, argsOnly = true)
    private int modifySubmitModelPart(int outlineColor) {
        return HandChamsRenderer.overrideOutlineColor(outlineColor);
    }

    @ModifyVariable(method = "submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V",
            at = @At("HEAD"), ordinal = 2, argsOnly = true)
    private int modifySubmitItem(int outlineColor) {
        return HandChamsRenderer.overrideOutlineColor(outlineColor);
    }
}
