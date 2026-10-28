package dev.echo.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.echo.client.bridge.WorldFramebufferControl;
import dev.echo.client.events.render.OutlineFramebufferEvent;
import dev.echo.render.HandChamsRenderer;
import java.util.Set;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class WorldRendererFramebufferMixin implements WorldFramebufferControl {
    @Shadow private RenderTarget entityOutlineTarget;
    @Shadow protected abstract boolean shouldShowEntityOutlines();
    @Unique private boolean echo$skipNextOutlineDraw;

    @WrapOperation(method = "render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ShaderManager;getPostChain(Lnet/minecraft/resources/Identifier;Ljava/util/Set;)Lnet/minecraft/client/renderer/PostChain;"))
    private PostChain loadOutlinePostEffect(ShaderManager loader, Identifier id,
            Set<Identifier> framebufferIds, Operation<PostChain> original) {
        OutlineFramebufferEvent.Render event = new OutlineFramebufferEvent.Render();
        event.post();
        return event.cancelled ? null : original.call(loader, id, framebufferIds);
    }

    @Inject(method = "doEntityOutline", at = @At("HEAD"), cancellable = true)
    private void cancelPreparedOutlineDraw(CallbackInfo callback) {
        if (echo$skipNextOutlineDraw) {
            echo$skipNextOutlineDraw = false;
            callback.cancel();
        }
    }

    @Inject(method = "entityOutlineTarget", at = @At("RETURN"), cancellable = true)
    private void useHandChamsFramebuffer(CallbackInfoReturnable<RenderTarget> callback) {
        if (HandChamsRenderer.isFlushingOutlines()) {
            callback.setReturnValue(HandChamsRenderer.getFramebuffer());
        }
    }

    @Override
    public void updateOutlineFramebufferCancellation() {
        OutlineFramebufferEvent.Prepare event = new OutlineFramebufferEvent.Prepare(
                entityOutlineTarget, shouldShowEntityOutlines());
        event.post();
        echo$skipNextOutlineDraw = event.cancelled;
    }
}
