package dev.echo.render.opengl;

import dev.echo.render.command.CommandEncoder;
import dev.echo.render.queue.CommandQueue;
import dev.echo.render.surface.PresentMode;
import dev.echo.render.surface.Surface;
import dev.echo.render.surface.SurfaceInfo;
import dev.echo.render.texture.TextureFormat;
import dev.echo.render.texture.TextureView;
import dev.echo.render.window.WindowSystem;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL32C;

/** Offscreen rendering followed by a fenced, vertically flipped blit on the window context. */
public class OpenGlSurface extends Surface {
    private final long windowHandle;
    private final WindowSystem windowSystem;
    private final TextureFormat format;
    private OpenGlTexture texture;
    private int readFramebuffer;
    private int width;
    private int height;

    public OpenGlSurface(SurfaceInfo info) {
        windowHandle = info.windowHandle;
        windowSystem = info.windowSystem;
        format = info.format;
    }

    @Override
    public void resize(int width, int height, PresentMode presentMode) {
        destroyAttachments();
        this.width = width;
        this.height = height;
        texture = new OpenGlTexture(width, height, 1, format, 9, 1);
        long fence = GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        GL11C.glFlush();
        long previousContext = windowSystem.currentContextWindow();
        windowSystem.makeContextCurrent(windowHandle);
        GL32C.glWaitSync(fence, 0, GL32C.GL_TIMEOUT_IGNORED);
        readFramebuffer = GL32C.glGenFramebuffers();
        GL32C.glBindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, readFramebuffer);
        GL32C.glFramebufferTexture2D(GL32C.GL_READ_FRAMEBUFFER, GL32C.GL_COLOR_ATTACHMENT0,
                GL32C.GL_TEXTURE_2D, texture.textureId, 0);
        GL32C.glBindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, 0);
        windowSystem.setSwapInterval(presentMode == PresentMode.IMMEDIATE ? 0 : 1);
        windowSystem.makeContextCurrent(previousContext);
        GL32C.glDeleteSync(fence);
    }

    @Override
    public boolean needsResize() { return false; }

    @Override
    public TextureView acquireTexture() { return texture.views[0]; }

    @Override
    public void present(CommandQueue queue, CommandEncoder commands) {
        queue.submit(commands);
        long fence = GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        GL11C.glFlush();
        long previousContext = windowSystem.currentContextWindow();
        windowSystem.makeContextCurrent(windowHandle);
        GL32C.glWaitSync(fence, 0, GL32C.GL_TIMEOUT_IGNORED);
        GL32C.glBindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, readFramebuffer);
        GL32C.glBindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, 0);
        GL32C.glBlitFramebuffer(0, 0, width, height, 0, height, width, 0,
                GL32C.GL_COLOR_BUFFER_BIT, GL32C.GL_NEAREST);
        GL32C.glBindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, 0);
        windowSystem.swapBuffers(windowHandle);
        windowSystem.makeContextCurrent(previousContext);
        GL32C.glDeleteSync(fence);
    }

    @Override
    public void destroy() { destroyAttachments(); }

    private void destroyAttachments() {
        if (readFramebuffer != 0) {
            long previousContext = windowSystem.currentContextWindow();
            windowSystem.makeContextCurrent(windowHandle);
            GL32C.glDeleteFramebuffers(readFramebuffer);
            windowSystem.makeContextCurrent(previousContext);
            readFramebuffer = 0;
        }
        if (texture != null) {
            texture.destroy();
            texture = null;
        }
    }
}
