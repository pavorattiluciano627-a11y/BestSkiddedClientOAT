package dev.echo.client.render;

import com.mojang.blaze3d.opengl.GlConst;
import dev.echo.render.opengl.GlFramebufferAccess;
import net.minecraft.client.Minecraft;

/** Candidate shared interface implementation; renderer-core-05 owns GlFramebufferAccess. */
public final class GlFramebufferBridge implements GlFramebufferAccess {
    @Override
    public int depthInternalFormat() {
        return GlConst.toGlInternalId(Minecraft.getInstance().gameRenderer.mainRenderTarget().getDepthTexture().getFormat());
    }

    @Override
    public void bindFramebuffer(int target, int framebuffer) {
        GlFramebufferOperations.bindFramebuffer(target, framebuffer);
    }

    @Override
    public void framebufferTexture2D(int target, int attachment, int textureTarget, int texture, int mipLevel) {
        GlFramebufferOperations.framebufferTexture2D(target, attachment, textureTarget, texture, mipLevel);
    }

    @Override
    public void deleteFramebuffer(int framebuffer) {
        GlFramebufferOperations.deleteFramebuffer(framebuffer);
    }
}
