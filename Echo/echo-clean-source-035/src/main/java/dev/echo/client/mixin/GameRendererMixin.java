package dev.echo.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.echo.client.Echo;
import dev.echo.client.bridge.GameRenderControl;
import dev.echo.client.bridge.GuiRenderControl;
import dev.echo.client.bridge.WorldFramebufferControl;
import dev.echo.client.events.render.WorldRenderEvent;
import dev.echo.render.FullscreenEffects;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.render.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements GameRenderControl {
    @Shadow @Final private FogRenderer fogRenderer;
    @Shadow @Final private GuiRenderer guiRenderer;
    @Shadow protected abstract void bobView(MatrixStack matrices, float tickProgress);
    @Shadow protected abstract void tiltViewWhenHurt(MatrixStack matrices, float tickProgress);

    @Inject(method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", ordinal = 0))
    private void captureProjection(RenderTickCounter ticks, CallbackInfo callback, @Local(ordinal = 0) Matrix4f projection) {
        FullscreenEffects.captureProjection(projection);
    }

    @Inject(method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", ordinal = 2))
    private void renderWorldOverlays(RenderTickCounter ticks, CallbackInfo callback,
            @Local(ordinal = 0) float tickProgress, @Local(ordinal = 0) Matrix4f projection,
            @Local(ordinal = 1) Matrix4f view, @Local MatrixStack matrices) {
        if (!Echo.eventsDisabled && Echo.worldRenderer != null && Echo.commandEncoder != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            Camera camera = MinecraftRenderState.getCamera(MinecraftClient.getInstance());
            RenderSystem.getModelViewStack().pushMatrix().mul(view);
            MatrixStack viewEffects = new MatrixStack();
            echo$applyViewEffects(viewEffects, tickProgress);
            RenderSystem.getModelViewStack().mul(viewEffects.peek().getPositionMatrix().invert());
            WorldRenderContext context = Echo.worldRenderer;
            context.setMatrices(view, projection);
            context.beginFrame();
            new WorldRenderEvent(matrices, tickProgress, ticks.getDynamicDeltaTicks(), camera,
                    (GameRenderer) (Object) this, projection, view, client.worldRenderer, context).post();
            context.flush();
            RenderSystem.getModelViewStack().popMatrix();
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/RenderTickCounter;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;drawEntityOutlinesFramebuffer()V"))
    private void prepareEntityOutlines(CallbackInfo callback) {
        ((WorldFramebufferControl) MinecraftClient.getInstance().worldRenderer).updateOutlineFramebufferCancellation();
    }

    @Inject(method = "render(Lnet/minecraft/client/render/RenderTickCounter;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
    private void beginMainGuiPass(CallbackInfo callback) {
        ((GuiRenderControl) guiRenderer).setMainGuiPass(true);
    }

    @Inject(method = "render(Lnet/minecraft/client/render/RenderTickCounter;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER))
    private void endMainGuiPass(CallbackInfo callback) {
        ((GuiRenderControl) guiRenderer).setMainGuiPass(false);
    }

    @Inject(method = "onResized", at = @At("HEAD"), cancellable = true)
    private void deferResizeWhileOverlayPresent(int width, int height, CallbackInfo callback) {
        if (MinecraftClient.getInstance() != null && MinecraftRenderState.getOverlay() != null) {
            callback.cancel();
        }
    }

    @Override
    public void renderGuiWithoutFog() {
        guiRenderer.render(fogRenderer.getFogBuffer(FogRenderer.FogType.NONE));
    }

    @Unique
    private void echo$applyViewEffects(MatrixStack matrices, float tickProgress) {
        tiltViewWhenHurt(matrices, tickProgress);
        if (MinecraftClient.getInstance().options.getBobView().getValue()) {
            bobView(matrices, tickProgress);
        }
    }
}
