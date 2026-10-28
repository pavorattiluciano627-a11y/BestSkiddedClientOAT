package dev.echo.render.window;

import org.lwjgl.vulkan.VkInstance;

/** Native window/context operations implemented independently by GLFW and SDL. */
public interface WindowSystem {
    /** Creates a surface for the supplied native window and Vulkan instance. */
    long createVulkanSurface(VkInstance instance, long window);

    /** GLFW's current context window, or SDL's current GL window. */
    long currentContextWindow();

    /** SDL preserves its current GL context when selecting the supplied window. */
    void makeContextCurrent(long window);

    void setSwapInterval(int interval);

    void swapBuffers(long window);

    /** Returns the native Cocoa window using the selected window-system adapter. */
    long nativeCocoaWindow(long window);

    int refreshRate(long window);
}
