package dev.echo.render.opengl;

import dev.echo.render.texture.TextureView;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL43C;

/** A GL 4.3 texture view when available, otherwise a borrowed parent texture ID. */
public class OpenGlTextureView extends TextureView {
    public final int textureId;

    public OpenGlTextureView(OpenGlTexture texture, int baseMipLevel, int mipCount) {
        this(texture, baseMipLevel, mipCount, true);
    }

    public OpenGlTextureView(OpenGlTexture texture, int baseMipLevel, int mipCount, boolean allocateStorage) {
        super(texture, baseMipLevel, mipCount);
        if (allocateStorage) {
            int width = Math.max(1, texture.width >> baseMipLevel);
            int height = Math.max(1, texture.height >> baseMipLevel);
            if (texture.target == GL30C.GL_TEXTURE_3D) {
                int depth = Math.max(1, texture.depth >> baseMipLevel);
                GL30C.glTexImage3D(texture.target, baseMipLevel, texture.internalFormat, width, height,
                        depth, 0, texture.pixelFormat, texture.pixelType, (ByteBuffer) null);
            } else {
                GL30C.glTexImage2D(texture.target, baseMipLevel, texture.internalFormat, width, height,
                        0, texture.pixelFormat, texture.pixelType, (ByteBuffer) null);
            }
        }
        if (texture.immutableStorage && GL.getCapabilities().OpenGL43) {
            int previousTexture = GL30C.glGetInteger(OpenGlTexture.bindingQuery(texture.target));
            int viewId = GL30C.glGenTextures();
            GL43C.glTextureView(viewId, texture.target, texture.textureId, texture.internalFormat,
                    baseMipLevel, mipCount, 0, 1);
            GL30C.glBindTexture(texture.target, viewId);
            GL30C.glTexParameteri(texture.target, GL30C.GL_TEXTURE_BASE_LEVEL, 0);
            GL30C.glTexParameteri(texture.target, GL30C.GL_TEXTURE_MAX_LEVEL, mipCount - 1);
            GL30C.glBindTexture(texture.target, previousTexture);
            textureId = viewId;
        } else {
            textureId = texture.textureId;
        }
    }

    public boolean ownsTextureId() {
        return textureId != ((OpenGlTexture) texture).textureId;
    }

    @Override
    public void destroy() {
        if (ownsTextureId()) GL30C.glDeleteTextures(textureId);
    }
}
