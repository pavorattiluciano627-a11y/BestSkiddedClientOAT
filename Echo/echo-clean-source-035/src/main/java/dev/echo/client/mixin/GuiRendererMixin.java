package dev.echo.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.echo.client.Echo;
import dev.echo.client.bridge.GuiRenderControl;
import dev.echo.client.bridge.GuiStateControl;
import dev.echo.client.events.render.HudRenderEvent;
import dev.echo.client.handlers.RotationPriority;
import dev.echo.client.render.DrawContextAdapter;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.render.FramebufferBlur;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.screens.EchoScreen;
import dev.echo.client.render.PlayerPreviewRenderer;
import dev.echo.render.command.CommandEncoder;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.IndexType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Analysis candidate; original caller/cache domains remain in the source receipt. */
@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin implements GuiRenderControl {
    @Shadow @Final private GuiRenderState renderState;
    @Unique private boolean echo$mainGuiPass;
    @Unique private boolean echo$pendingHud;
    @Unique private boolean echo$drawHudBeforeBlur;
    @Unique private boolean echo$mirrorDraws;

    /**
     * Original ChannelManager.setPath, specialized to the closed active-client
     * null-state lemma for AuditMapper.temporaryView. Keep the enum and Screen
     * initialization preceding the screen query, as in the fixed-join fragment.
     */
    @Unique
    private static boolean echo$shouldDeferHud() {
        try {
            Class.forName(RotationPriority.class.getName(), true, RotationPriority.class.getClassLoader());
            Class.forName(Screen.class.getName(), true, Screen.class.getClassLoader());
        } catch (ClassNotFoundException missingResolvedClass) {
            throw new IllegalStateException("Resolved class initialization failed", missingResolvedClass);
        }
        Screen screen = MinecraftRenderState.getScreen();
        if (screen instanceof EchoScreen) {
            return true;
        }
        return screen == null && !MinecraftRenderState.hasOverlay();
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;prepare()V"))
    private void prepareHudPass(CallbackInfo callback) {
        echo$pendingHud = false;
        echo$drawHudBeforeBlur = false;
        echo$mirrorDraws = false;
        if (echo$mainGuiPass && !Echo.eventsDisabled && Echo.uiRenderer != null && Echo.commandEncoder != null
                && Minecraft.getInstance().level != null) {
            Echo.uiRenderer.renderPendingItems();
            PlayerPreviewRenderer.beginFrame();
            if (!echo$shouldDeferHud() && MinecraftRenderState.hasOverlay()) {
                if (!Echo.externalRendererActive) {
                    echo$renderHud(false);
                }
            } else {
                echo$pendingHud = true;
                if (!echo$shouldDeferHud()) {
                    echo$drawHudBeforeBlur = ((GuiStateControl) renderState).prepareScreenBlurLayer();
                }
            }
        }
    }

    @WrapOperation(method = "draw", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V"))
    private void renderHudAtBlur(GameRenderer renderer, Operation<Void> original) {
        if (echo$drawHudBeforeBlur) {
            echo$drawHudBeforeBlur = false;
            echo$pendingHud = false;
            if (Echo.externalRendererActive) {
                echo$mirrorDraws = true;
                echo$renderHudToOverlay();
            } else {
                echo$renderHud(false);
            }
        }
        if (!((GuiStateControl) renderState).isScreenBlurLayerInstalled()) {
            original.call(renderer);
            if (echo$mirrorDraws) {
                Echo.externalRenderer.framebufferBlur().applyBlur();
            }
        }
    }

    @WrapOperation(method = "draw", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V", ordinal = 1))
    private void mirrorGuiDraw(GuiRenderer renderer, Supplier<String> label, RenderTarget target,
            GpuBufferSlice dynamicTransforms, GpuBufferSlice fog, GpuBuffer indices,
            IndexType indexType, int start, int end, Operation<Void> original) {
        original.call(renderer, label, target, dynamicTransforms, fog, indices, indexType, start, end);
        if (echo$mirrorDraws) {
            RenderTarget overlay = Echo.externalRenderer.framebufferBlur().framebuffer();
            original.call(renderer, label, overlay, dynamicTransforms, fog, indices, indexType, start, end);
        }
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;draw(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER))
    private void finishPendingHud(CallbackInfo callback) {
        if (echo$pendingHud) {
            echo$pendingHud = false;
            echo$drawHudBeforeBlur = false;
            echo$renderHud(MinecraftRenderState.getScreen() instanceof EchoScreen);
        }
    }

    @Unique
    private void echo$renderHud(boolean updateBackdrop) {
        UiRenderer renderer = Echo.uiRenderer;
        if (updateBackdrop) {
            renderer.updateBackdrop();
        }
        renderer.beginFrame();
        echo$postHudEvent();
        renderer.endFrame();
    }

    @Unique
    private void echo$renderHudToOverlay() {
        FramebufferBlur overlay = Echo.externalRenderer.framebufferBlur();
        overlay.captureMainFramebuffer();
        CommandEncoder encoder = Echo.commandEncoder;
        Echo.worldRenderer.beginTarget(encoder, overlay.colorTexture(), OptionalInt.empty());
        Echo.uiRenderer.beginFrame(encoder, overlay.colorTexture(), overlay.depthTexture(), OptionalInt.empty());
        echo$postHudEvent();
        Echo.uiRenderer.endFrame();
    }

    @Unique
    private void echo$postHudEvent() {
        Minecraft client = Minecraft.getInstance();
        int mouseX = (int) client.mouseHandler.getScaledXPos(client.getWindow());
        int mouseY = (int) client.mouseHandler.getScaledYPos(client.getWindow());
        GuiGraphicsExtractor drawContext = new GuiGraphicsExtractor(client, renderState, mouseX, mouseY);
        UiRenderer renderer = Echo.uiRenderer;
        new HudRenderEvent(new PoseStack(), client.getDeltaTracker().getGameTimeDeltaPartialTick(true),
                DrawContextAdapter.wrap(drawContext), renderer, renderer.backdropTexture).post();
    }

    @Override
    public void setMainGuiPass(boolean mainGuiPass) {
        echo$mainGuiPass = mainGuiPass;
    }
}
