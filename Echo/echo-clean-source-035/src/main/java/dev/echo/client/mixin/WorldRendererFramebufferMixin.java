package dev.echo.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.echo.client.bridge.WorldFramebufferControl;
import dev.echo.client.events.render.OutlineFramebufferEvent;
import dev.echo.render.HandChamsRenderer;
import java.util.Set;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererFramebufferMixin implements WorldFramebufferControl {
    @Shadow private Framebuffer entityOutlineFramebuffer;
    @Shadow protected abstract boolean canDrawEntityOutlines();
    @Unique private boolean echo$skipNextOutlineDraw;

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gl/ShaderLoader;loadPostEffect(Lnet/minecraft/util/Identifier;Ljava/util/Set;)Lnet/minecraft/client/gl/PostEffectProcessor;"))
    private PostEffectProcessor loadOutlinePostEffect(ShaderLoader loader, Identifier id,
            Set<Identifier> framebufferIds, Operation<PostEffectProcessor> original) {
        OutlineFramebufferEvent.Render event = new OutlineFramebufferEvent.Render();
        event.post();
        return event.cancelled ? null : original.call(loader, id, framebufferIds);
    }

    @Inject(method = "drawEntityOutlinesFramebuffer", at = @At("HEAD"), cancellable = true)
    private void cancelPreparedOutlineDraw(CallbackInfo callback) {
        if (echo$skipNextOutlineDraw) {
            echo$skipNextOutlineDraw = false;
            callback.cancel();
        }
    }

    @Inject(method = "getEntityOutlinesFramebuffer", at = @At("RETURN"), cancellable = true)
    private void useHandChamsFramebuffer(CallbackInfoReturnable<Framebuffer> callback) {
        if (HandChamsRenderer.isFlushingOutlines()) {
            callback.setReturnValue(HandChamsRenderer.getFramebuffer());
        }
    }

    @Override
    public void updateOutlineFramebufferCancellation() {
        OutlineFramebufferEvent.Prepare event = new OutlineFramebufferEvent.Prepare(
                entityOutlineFramebuffer, canDrawEntityOutlines());
        event.post();
        echo$skipNextOutlineDraw = event.cancelled;
    }
}
