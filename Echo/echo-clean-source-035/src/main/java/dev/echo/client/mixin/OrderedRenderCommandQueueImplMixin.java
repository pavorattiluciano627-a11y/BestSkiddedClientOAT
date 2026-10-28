package dev.echo.client.mixin;

import dev.echo.render.HandChamsRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Candidate: key-free outline-color transform requires HandChamsRenderer callee proof. */
@Mixin(OrderedRenderCommandQueueImpl.class)
public class OrderedRenderCommandQueueImplMixin {
    @ModifyVariable(method = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V",
            at = @At("HEAD"), ordinal = 3, argsOnly = true)
    private int modifySubmitModel(int outlineColor) {
        return HandChamsRenderer.overrideOutlineColor(outlineColor);
    }

    @ModifyVariable(method = "submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;ZZILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V",
            at = @At("HEAD"), ordinal = 3, argsOnly = true)
    private int modifySubmitModelPart(int outlineColor) {
        return HandChamsRenderer.overrideOutlineColor(outlineColor);
    }

    @ModifyVariable(method = "submitItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V",
            at = @At("HEAD"), ordinal = 2, argsOnly = true)
    private int modifySubmitItem(int outlineColor) {
        return HandChamsRenderer.overrideOutlineColor(outlineColor);
    }
}
