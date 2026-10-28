package dev.echo.render.window;

import java.nio.LongBuffer;
import org.lwjgl.sdl.SDLProperties;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDLVulkan;
import org.lwjgl.sdl.SDL_DisplayMode;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkInstance;

/** Conditional reconstruction; surface/refresh guards and the WindowSystem contract remain pending. */
public final class SdlWindowSystem implements WindowSystem {
    @Override
    public long createVulkanSurface(VkInstance instance, long windowHandle) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer output = stack.mallocLong(1);
            if (!SDLVulkan.SDL_Vulkan_CreateSurface(windowHandle, instance, null, output)) {
                throw new IllegalStateException("Failed to create window surface");
            }
            return output.get(0);
        }
    }

    @Override
    public long currentContextWindow() {
        return JNI.invokeP(SDLVideo.Functions.GL_GetCurrentWindow);
    }

    @Override
    public void makeContextCurrent(long windowHandle) {
        SDLVideo.SDL_GL_MakeCurrent(windowHandle, SDLVideo.SDL_GL_GetCurrentContext());
    }

    @Override
    public void setSwapInterval(int interval) {
        SDLVideo.SDL_GL_SetSwapInterval(interval);
    }

    @Override
    public void swapBuffers(long windowHandle) {
        SDLVideo.SDL_GL_SwapWindow(windowHandle);
    }

    @Override
    public long nativeCocoaWindow(long windowHandle) {
        return SDLProperties.SDL_GetPointerProperty(SDLVideo.SDL_GetWindowProperties(windowHandle),
                "SDL.window.cocoa.window", 0L);
    }

    @Override
    public int refreshRate(long windowHandle) {
        SDL_DisplayMode mode = SDLVideo.SDL_GetCurrentDisplayMode(SDLVideo.SDL_GetDisplayForWindow(windowHandle));
        return mode == null || mode.refresh_rate() <= 0.0f ? 60 : Math.round(mode.refresh_rate());
    }
}
