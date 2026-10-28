package dev.echo.render.opengl;

import dev.echo.render.texture.Texture;
import dev.echo.render.texture.TextureFormat;
import dev.echo.render.texture.TextureView;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL42C;

/** GL storage and its per-mip views. Imported IDs retain the reference's destroy semantics. */
public class OpenGlTexture extends Texture {
    public final int textureId;
    public final int internalFormat;
    public final int pixelFormat;
    public final int pixelType;
    public final boolean imported;
    public final boolean immutableStorage;
    public final int target;

    public OpenGlTexture(int width, int height, int depth, TextureFormat format, int usage, int mipCount) {
        super(width, height, depth, format, usage, mipCount);
        target = depth > 1 ? GL30C.GL_TEXTURE_3D : GL30C.GL_TEXTURE_2D;
        internalFormat = OpenGlTextureFormats.internalFormat(format);
        pixelFormat = OpenGlTextureFormats.pixelFormat(format);
        pixelType = OpenGlTextureFormats.pixelType(format);
        imported = false;
        textureId = GL30C.glGenTextures();
        immutableStorage = (usage & 2) != 0;
        int previousTexture = GL30C.glGetInteger(bindingQuery(target));
        GL30C.glBindTexture(target, textureId);
        if (immutableStorage) {
            if (target == GL30C.GL_TEXTURE_3D) {
                GL42C.glTexStorage3D(target, mipCount, internalFormat, width, height, depth);
            } else {
                GL42C.glTexStorage2D(target, mipCount, internalFormat, width, height);
            }
        }
        views = new TextureView[mipCount];
        for (int mip = 0; mip < mipCount; mip++) {
            views[mip] = new OpenGlTextureView(this, mip, 1, !immutableStorage);
        }
        GL30C.glTexParameteri(target, GL30C.GL_TEXTURE_BASE_LEVEL, 0);
        GL30C.glTexParameteri(target, GL30C.GL_TEXTURE_MAX_LEVEL, mipCount - 1);
        GL30C.glBindTexture(target, previousTexture);
    }

    public static int bindingQuery(int target) {
        return target == GL30C.GL_TEXTURE_3D ? GL30C.GL_TEXTURE_BINDING_3D : GL30C.GL_TEXTURE_BINDING_2D;
    }

    public static OpenGlTexture importTexture(int width, int height, TextureFormat format,
                                             int textureId, int internalFormat) {
        return new OpenGlTexture(width, height, format, textureId, internalFormat, true);
    }

    private OpenGlTexture(int width, int height, TextureFormat format, int textureId,
                          int internalFormat, boolean imported) {
        super(width, height, format, 1, 1);
        this.internalFormat = internalFormat;
        pixelFormat = OpenGlTextureFormats.pixelFormat(format);
        pixelType = OpenGlTextureFormats.pixelType(format);
        this.textureId = textureId;
        this.imported = imported;
        immutableStorage = false;
        target = GL30C.GL_TEXTURE_2D;
        views = new OpenGlTextureView[]{new OpenGlTextureView(this, 0, 1, false)};
    }

    @Override
    public void destroy() {
        for (TextureView view : views) {
            if (view instanceof OpenGlTextureView glView) glView.destroy();
        }
        // Original destroy does not test the imported flag; that flag controls binding paths.
        GL30C.glDeleteTextures(textureId);
    }
}
