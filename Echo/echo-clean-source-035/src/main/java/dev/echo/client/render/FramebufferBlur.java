package dev.echo.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.echo.client.Echo;
import dev.echo.client.mixin.GameRendererAccessor;
import dev.echo.render.texture.TextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.util.Identifier;

/**
 * Captures the main color attachment and applies Minecraft's blur post-effect.
 * String values are recovered conditional plaintext; dispatch/cache applicability
 * and the original static initializer remain separately gated in the proof ledger.
 */
public final class FramebufferBlur implements AutoCloseable {
    private static final Identifier EFFECT = Identifier.ofVanilla("blur");
    private static final String FRAMEBUFFER_NAME = "echo_screen_frame";

    private Framebuffer framebuffer;
    private TextureView colorTexture;
    private TextureView depthTexture;
    private boolean active;
    private GpuTextureView importedColorAttachment;

    public void captureMainFramebuffer() {
        Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
        if (framebuffer == null) {
            framebuffer = new SimpleFramebuffer(FRAMEBUFFER_NAME, main.textureWidth, main.textureHeight, true);
        } else if (framebuffer.textureWidth != main.textureWidth || framebuffer.textureHeight != main.textureHeight) {
            framebuffer.resize(main.textureWidth, main.textureHeight);
        }
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                main.getColorAttachment(), framebuffer.getColorAttachment(),
                0, 0, 0, 0, 0, main.textureWidth, main.textureHeight);

        if (framebuffer.getColorAttachmentView() != importedColorAttachment) {
            importedColorAttachment = framebuffer.getColorAttachmentView();
            colorTexture = GlTextureBridge.importColor(Echo.renderDevice, importedColorAttachment);
            depthTexture = GlTextureBridge.importView(Echo.renderDevice, framebuffer.getDepthAttachmentView(),
                    Echo.renderDevice.depthFormat());
        }
        active = true;
    }

    public void applyBlur() {
        PostEffectProcessor effect = MinecraftClient.getInstance().getShaderLoader()
                .loadPostEffect(EFFECT, DefaultFramebufferSet.MAIN_ONLY);
        if (effect == null) {
            return;
        }
        effect.render(framebuffer, ((GameRendererAccessor) MinecraftClient.getInstance().gameRenderer).getPool());
    }

    public void resetFrame() {
        active = false;
    }

    @Override
    public void close() {
        if (framebuffer != null) {
            framebuffer.delete();
        }
    }

    public Framebuffer framebuffer() { return framebuffer; }
    public TextureView colorTexture() { return colorTexture; }
    public TextureView depthTexture() { return depthTexture; }
    public boolean isActive() { return active; }
}
