package dev.echo.render.window;

import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeCocoa;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkInstance;

/** Conditional reconstruction; monitor/refresh/surface guard proofs and shared API merge are pending. */
public final class GlfwWindowSystem implements WindowSystem {
    @Override
    public long createVulkanSurface(VkInstance instance, long windowHandle) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer output = stack.mallocLong(1);
            int result = GLFWVulkan.glfwCreateWindowSurface(instance, windowHandle, null, output);
            if (result != 0) {
                throw new IllegalStateException("Failed to create window surface: " + result);
            }
            return output.get(0);
        }
    }

    @Override
    public long currentContextWindow() {
        return GLFW.glfwGetCurrentContext();
    }

    @Override
    public void makeContextCurrent(long windowHandle) {
        GLFW.glfwMakeContextCurrent(windowHandle);
    }

    @Override
    public void setSwapInterval(int interval) {
        GLFW.glfwSwapInterval(interval);
    }

    @Override
    public void swapBuffers(long windowHandle) {
        GLFW.glfwSwapBuffers(windowHandle);
    }

    @Override
    public long nativeCocoaWindow(long windowHandle) {
        return GLFWNativeCocoa.glfwGetCocoaWindow(windowHandle);
    }

    @Override
    public int refreshRate(long windowHandle) {
        long monitor = findMonitor(windowHandle);
        GLFWVidMode mode = monitor == 0L ? null : GLFW.glfwGetVideoMode(monitor);
        return mode == null || mode.refreshRate() <= 0 ? 60 : mode.refreshRate();
    }

    private static long findMonitor(long windowHandle) {
        long fullscreenMonitor = GLFW.glfwGetWindowMonitor(windowHandle);
        if (fullscreenMonitor != 0L) {
            return fullscreenMonitor;
        }
        int[] windowX = new int[1];
        int[] windowY = new int[1];
        GLFW.glfwGetWindowPos(windowHandle, windowX, windowY);
        PointerBuffer monitors = GLFW.glfwGetMonitors();
        if (monitors != null) {
            for (int index = 0; index < monitors.limit(); index++) {
                long monitor = monitors.get(index);
                GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
                if (mode == null) {
                    continue;
                }
                int[] monitorX = new int[1];
                int[] monitorY = new int[1];
                GLFW.glfwGetMonitorPos(monitor, monitorX, monitorY);
                // Original selection uses the window's top-left point, not maximum overlap.
                if (windowX[0] >= monitorX[0] && windowX[0] < monitorX[0] + mode.width()
                        && windowY[0] >= monitorY[0] && windowY[0] < monitorY[0] + mode.height()) {
                    return monitor;
                }
            }
        }
        return GLFW.glfwGetPrimaryMonitor();
    }
}
