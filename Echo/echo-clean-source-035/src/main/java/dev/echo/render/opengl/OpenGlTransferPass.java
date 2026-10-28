package dev.echo.render.opengl;

import dev.echo.render.buffer.BufferSlice;
import dev.echo.render.command.TransferPass;
import dev.echo.render.texture.TextureView;
import org.lwjgl.opengl.GL33C;

/** Immediate CPU-staging uploads, preserving the caller's pixel unpack and texture bindings. */
public class OpenGlTransferPass extends TransferPass {
    @Override
    public void uploadTexture(TextureView destination, BufferSlice source) {
        OpenGlTextureView view = (OpenGlTextureView) destination;
        OpenGlTexture texture = (OpenGlTexture) view.texture;
        int mip = view.baseMipLevel;
        int width = view.width(0);
        int height = view.height(0);
        int depth = view.depth(0);
        int oldTexture = GL33C.glGetInteger(OpenGlTexture.bindingQuery(texture.target));
        int alignment = GL33C.glGetInteger(GL33C.GL_UNPACK_ALIGNMENT);
        int rowLength = GL33C.glGetInteger(GL33C.GL_UNPACK_ROW_LENGTH);
        int skipPixels = GL33C.glGetInteger(GL33C.GL_UNPACK_SKIP_PIXELS);
        int skipRows = GL33C.glGetInteger(GL33C.GL_UNPACK_SKIP_ROWS);
        int imageHeight = GL33C.glGetInteger(GL33C.GL_UNPACK_IMAGE_HEIGHT);
        int skipImages = GL33C.glGetInteger(GL33C.GL_UNPACK_SKIP_IMAGES);
        int unpackBuffer = GL33C.glGetInteger(GL33C.GL_PIXEL_UNPACK_BUFFER_BINDING);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_ALIGNMENT, 1);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_ROW_LENGTH, 0);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_SKIP_PIXELS, 0);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_SKIP_ROWS, 0);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_IMAGE_HEIGHT, 0);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_SKIP_IMAGES, 0);
        GL33C.glBindBuffer(GL33C.GL_PIXEL_UNPACK_BUFFER, 0);
        GL33C.glBindTexture(texture.target, texture.textureId);
        if (texture.target == GL33C.GL_TEXTURE_3D) {
            GL33C.nglTexSubImage3D(texture.target, mip, 0, 0, 0, width, height, depth,
                    texture.pixelFormat, texture.pixelType, source.buffer().mappedAddress() + source.offset());
        } else {
            GL33C.nglTexSubImage2D(texture.target, mip, 0, 0, width, height,
                    texture.pixelFormat, texture.pixelType, source.buffer().mappedAddress() + source.offset());
        }
        GL33C.glBindTexture(texture.target, oldTexture);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_ALIGNMENT, alignment);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_ROW_LENGTH, rowLength);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_SKIP_PIXELS, skipPixels);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_SKIP_ROWS, skipRows);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_IMAGE_HEIGHT, imageHeight);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_SKIP_IMAGES, skipImages);
        GL33C.glBindBuffer(GL33C.GL_PIXEL_UNPACK_BUFFER, unpackBuffer);
    }

    @Override
    public void end() {
        // Proven return-only in the original: GL uploads execute immediately.
    }
}
