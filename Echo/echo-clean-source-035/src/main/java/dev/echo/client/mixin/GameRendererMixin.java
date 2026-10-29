package dev.echo.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.echo.client.Echo;
import dev.echo.client.bridge.GameRenderControl;
import dev.echo.client.bridge.GuiRenderControl;
import dev.echo.client.bridge.WorldFramebufferControl;
import dev.echo.client.events.render.WorldRenderEvent;
import dev.echo.render.FullscreenEffects;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.render.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.fog.FogRenderer;
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
    // 26.2 sources view-bob from the extracted camera render state, not from a
    // tick delta: bobView/bobHurt(CameraRenderState, PoseStack).
    @Shadow protected abstract void bobView(CameraRenderState camera, PoseStack matrices);
    @Shadow protected abstract void bobHurt(CameraRenderState camera, PoseStack matrices);

    @Inject(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", ordinal = 0), require = 0)
    private void captureProjection(DeltaTracker ticks, CallbackInfo callback, @Local(ordinal = 0) Matrix4f projection) {
        FullscreenEffects.captureProjection(projection);
    }

    @Inject(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", ordinal = 2), require = 0)
    private void renderWorldOverlays(DeltaTracker ticks, CallbackInfo callback,
            @Local(ordinal = 0) float tickProgress, @Local(ordinal = 0) Matrix4f projection,
            @Local(ordinal = 1) Matrix4f view, @Local PoseStack matrices) {
        if (!Echo.eventsDisabled && Echo.worldRenderer != null && Echo.commandEncoder != null) {
            Minecraft client = Minecraft.getInstance();
            Camera camera = MinecraftRenderState.getCamera(Minecraft.getInstance());
            RenderSystem.getModelViewStack().pushMatrix().mul(view);
            PoseStack viewEffects = new PoseStack();
            echo$applyViewEffects(viewEffects, tickProgress);
            RenderSystem.getModelViewStack().mul(viewEffects.last().pose().invert());
            WorldRenderContext context = Echo.worldRenderer;
            context.setMatrices(view, projection);
            context.beginFrame();
            new WorldRenderEvent(matrices, tickProgress, ticks.getGameTimeDeltaTicks(), camera,
                    (GameRenderer) (Object) this, projection, view, client.levelRenderer, context).post();
            context.flush();
            RenderSystem.getModelViewStack().popMatrix();
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V"), require = 0)
    private void prepareEntityOutlines(CallbackInfo callback) {
        ((WorldFramebufferControl) Minecraft.getInstance().levelRenderer).updateOutlineFramebufferCancellation();
    }

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V"), require = 1)
    private void beginMainGuiPass(CallbackInfo callback) {
        ((GuiRenderControl) guiRenderer).setMainGuiPass(true);
    }

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V", shift = At.Shift.AFTER), require = 1)
    private void endMainGuiPass(CallbackInfo callback) {
        ((GuiRenderControl) guiRenderer).setMainGuiPass(false);
    }

    @Inject(method = "resize", at = @At("HEAD"), cancellable = true)
    private void deferResizeWhileOverlayPresent(int width, int height, CallbackInfo callback) {
        if (Minecraft.getInstance() != null && MinecraftRenderState.getOverlay() != null) {
            callback.cancel();
        }
    }

    @Override
    public void renderGuiWithoutFog() {
        guiRenderer.render();
    }

    @Unique
    private void echo$applyViewEffects(PoseStack matrices, float tickProgress) {
        CameraRenderState camera = ((GameRenderer) (Object) this)
                .gameRenderState().levelRenderState.cameraRenderState;
        if (camera == null) {
            return;
        }
        bobHurt(camera, matrices);
        if (Minecraft.getInstance().options.bobView().get()) {
            bobView(camera, matrices);
        }
    }
}
