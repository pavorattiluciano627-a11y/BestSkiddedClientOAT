package dev.echo.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
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
    @Shadow @Final private GuiRenderState state;
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
                && MinecraftClient.getInstance().world != null) {
            Echo.uiRenderer.renderPendingItems();
            PlayerPreviewRenderer.beginFrame();
            if (!echo$shouldDeferHud() && MinecraftRenderState.hasOverlay()) {
                if (!Echo.externalRendererActive) {
                    echo$renderHud(false);
                }
            } else {
                echo$pendingHud = true;
                if (!echo$shouldDeferHud()) {
                    echo$drawHudBeforeBlur = ((GuiStateControl) state).prepareScreenBlurLayer();
                }
            }
        }
    }

    @WrapOperation(method = "renderPreparedDraws", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;renderBlur()V"))
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
        if (!((GuiStateControl) state).isScreenBlurLayerInstalled()) {
            original.call(renderer);
            if (echo$mirrorDraws) {
                Echo.externalRenderer.framebufferBlur().applyBlur();
            }
        }
    }

    @WrapOperation(method = "renderPreparedDraws", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Ljava/util/function/Supplier;Lnet/minecraft/client/gl/Framebuffer;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V", ordinal = 1))
    private void mirrorGuiDraw(GuiRenderer renderer, Supplier<String> label, Framebuffer target,
            GpuBufferSlice dynamicTransforms, GpuBufferSlice fog, GpuBuffer indices,
            VertexFormat.IndexType indexType, int start, int end, Operation<Void> original) {
        original.call(renderer, label, target, dynamicTransforms, fog, indices, indexType, start, end);
        if (echo$mirrorDraws) {
            Framebuffer overlay = Echo.externalRenderer.framebufferBlur().framebuffer();
            original.call(renderer, label, overlay, dynamicTransforms, fog, indices, indexType, start, end);
        }
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;renderPreparedDraws(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER))
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
        MinecraftClient client = MinecraftClient.getInstance();
        int mouseX = (int) client.mouse.getScaledX(client.getWindow());
        int mouseY = (int) client.mouse.getScaledY(client.getWindow());
        DrawContext drawContext = new DrawContext(client, state, mouseX, mouseY);
        UiRenderer renderer = Echo.uiRenderer;
        new HudRenderEvent(new MatrixStack(), client.getRenderTickCounter().getTickProgress(true),
                DrawContextAdapter.wrap(drawContext), renderer, renderer.backdropTexture).post();
    }

    @Override
    public void setMainGuiPass(boolean mainGuiPass) {
        echo$mainGuiPass = mainGuiPass;
    }
}
