package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.bridge.WindowControl;
import dev.echo.client.events.EventBus;
import dev.echo.client.events.window.WindowFocusEvent;
import dev.echo.client.events.window.WindowPositionEvent;
import dev.echo.client.events.window.WindowSizeEvent;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.Platform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class WindowMixin implements WindowControl {
    @Shadow private boolean fullscreen;
    @Shadow private boolean currentFullscreen;
    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int width;
    @Shadow private int height;
    @Unique private long echo$borderlessMonitor;

    @Override
    public void invalidateFullscreenState() {
        if (fullscreen) {
            currentFullscreen = false;
        }
    }

    @Redirect(method = "updateWindowRegion", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/glfw/GLFW;glfwGetWindowMonitor(J)J", remap = false))
    private long getEffectiveMonitor(long window) {
        return echo$borderlessMonitor != 0L ? echo$borderlessMonitor : GLFW.glfwGetWindowMonitor(window);
    }

    @Redirect(method = "updateWindowRegion", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowMonitor(JJIIIII)V", remap = false))
    private void applyBorderlessFullscreen(long window, long monitor, int requestedX, int requestedY,
            int requestedWidth, int requestedHeight, int refreshRate) {
        if (monitor != 0L && Echo.externalRendererActive && Platform.get() == Platform.WINDOWS) {
            GLFWVidMode videoMode = GLFW.glfwGetVideoMode(monitor);
            if (videoMode == null) {
                echo$restoreDecorations(window);
                GLFW.glfwSetWindowMonitor(window, monitor, requestedX, requestedY,
                        requestedWidth, requestedHeight, refreshRate);
                return;
            }
            int[] monitorX = new int[1];
            int[] monitorY = new int[1];
            GLFW.glfwGetMonitorPos(monitor, monitorX, monitorY);
            int fullscreenWidth = videoMode.width();
            int fullscreenHeight = videoMode.height() + 1;
            if (GLFW.glfwGetWindowMonitor(window) != 0L) {
                GLFW.glfwSetWindowMonitor(window, 0L, monitorX[0], monitorY[0],
                        fullscreenWidth, fullscreenHeight, GLFW.GLFW_DONT_CARE);
            }
            if (GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE) {
                GLFW.glfwRestoreWindow(window);
            }
            GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
            GLFW.glfwSetWindowMonitor(window, 0L, monitorX[0], monitorY[0],
                    fullscreenWidth, fullscreenHeight, GLFW.GLFW_DONT_CARE);
            echo$borderlessMonitor = monitor;
            x = monitorX[0];
            y = monitorY[0];
            width = fullscreenWidth;
            height = fullscreenHeight;
        } else {
            echo$restoreDecorations(window);
            GLFW.glfwSetWindowMonitor(window, monitor, requestedX, requestedY,
                    requestedWidth, requestedHeight, refreshRate);
        }
    }

    @Unique
    private void echo$restoreDecorations(long window) {
        if (echo$borderlessMonitor != 0L) {
            echo$borderlessMonitor = 0L;
            GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
        }
    }

    @Inject(method = "onWindowPosChanged", at = @At("TAIL"))
    void postWindowPosition(long window, int newX, int newY, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        EventBus.post(new WindowPositionEvent(newX, newY));
    }

    @Inject(method = "onWindowSizeChanged", at = @At("TAIL"))
    void postWindowSize(long window, int newWidth, int newHeight, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        EventBus.post(new WindowSizeEvent(newWidth, newHeight));
    }

    @Inject(method = "onWindowFocusChanged", at = @At("TAIL"))
    void postWindowFocus(long window, boolean focused, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        EventBus.post(new WindowFocusEvent(focused));
    }
}
