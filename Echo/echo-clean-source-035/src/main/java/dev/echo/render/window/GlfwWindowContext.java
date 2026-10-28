package dev.echo.render.window;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeCocoa;

/** Direct context operations on a borrowed GLFW window. Does not own or create the window. */
public final class GlfwWindowContext {
    public long currentContextWindow() {
        return GLFW.glfwGetCurrentContext();
    }

    public void makeContextCurrent(long windowHandle) {
        GLFW.glfwMakeContextCurrent(windowHandle);
    }

    public void setSwapInterval(int interval) {
        GLFW.glfwSwapInterval(interval);
    }

    public void swapBuffers(long windowHandle) {
        GLFW.glfwSwapBuffers(windowHandle);
    }

    public long nativeCocoaWindow(long windowHandle) {
        return GLFWNativeCocoa.glfwGetCocoaWindow(windowHandle);
    }
}
