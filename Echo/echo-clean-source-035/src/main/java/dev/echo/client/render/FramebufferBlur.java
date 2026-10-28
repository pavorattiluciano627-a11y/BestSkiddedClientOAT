package dev.echo.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.echo.client.Echo;
import dev.echo.client.mixin.GameRendererAccessor;
import dev.echo.render.texture.TextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;

/**
 * Captures the main color attachment and applies Minecraft's blur post-effect.
 * String values are recovered conditional plaintext; dispatch/cache applicability
 * and the original static initializer remain separately gated in the proof ledger.
 */
public final class FramebufferBlur implements AutoCloseable {
    private static final Identifier EFFECT = Identifier.withDefaultNamespace("blur");
    private static final String FRAMEBUFFER_NAME = "echo_screen_frame";

    private RenderTarget framebuffer;
    private TextureView colorTexture;
    private TextureView depthTexture;
    private boolean active;
    private GpuTextureView importedColorAttachment;

    public void captureMainFramebuffer() {
        RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        if (framebuffer == null) {
            framebuffer = new TextureTarget(FRAMEBUFFER_NAME, main.width, main.height, true,
                    com.mojang.blaze3d.GpuFormat.RGBA8_UNORM);
        } else if (framebuffer.width != main.width || framebuffer.height != main.height) {
            framebuffer.resize(main.width, main.height);
        }
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                main.getColorTexture(), framebuffer.getColorTexture(),
                0, 0, 0, 0, 0, main.width, main.height);

        if (framebuffer.getColorTextureView() != importedColorAttachment) {
            importedColorAttachment = framebuffer.getColorTextureView();
            colorTexture = GlTextureBridge.importColor(Echo.renderDevice, importedColorAttachment);
            depthTexture = GlTextureBridge.importView(Echo.renderDevice, framebuffer.getDepthTextureView(),
                    Echo.renderDevice.depthFormat());
        }
        active = true;
    }

    public void applyBlur() {
        PostChain effect = Minecraft.getInstance().getShaderManager()
                .getPostChain(EFFECT, LevelTargetBundle.MAIN_TARGETS);
        if (effect == null) {
            return;
        }
        effect.process(framebuffer, ((GameRendererAccessor) Minecraft.getInstance().gameRenderer).getPool());
    }

    public void resetFrame() {
        active = false;
    }

    @Override
    public void close() {
        if (framebuffer != null) {
            framebuffer.destroyBuffers();
        }
    }

    public RenderTarget framebuffer() { return framebuffer; }
    public TextureView colorTexture() { return colorTexture; }
    public TextureView depthTexture() { return depthTexture; }
    public boolean isActive() { return active; }
}
