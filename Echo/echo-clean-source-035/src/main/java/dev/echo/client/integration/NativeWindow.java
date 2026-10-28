package dev.echo.client.integration;

import dev.echo.client.events.EventBus;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.window.WindowFocusEvent;
import dev.echo.client.events.window.WindowPositionEvent;
import dev.echo.client.events.window.WindowSizeEvent;
import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeCocoa;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.lwjgl.system.SharedLibrary;
import org.lwjgl.system.macosx.ObjCRuntime;

/**
 * Owned transparent render window, synchronized and attached to the main window.
 * This source candidate retains explicit key/cache and platform-validation
 * obligations in the provider receipt; no unconditional binding is asserted.
 */
public final class NativeWindow implements NativeWindowBackend {
    private static final long OWNER_WINDOW_INDEX = -8L;
    private static final long EXCLUDE_FROM_CAPTURE = 17L;
    private static final long WINDOW_ABOVE = 1L;
    private static final long SHARING_NONE = 0L;
    private final long window;
    private final long mainWindow;
    private final int[] horizontal = new int[1];
    private final int[] vertical = new int[1];
    private int x;
    private int y;
    private int width;
    private int height;
    private int framebufferWidth;
    private int framebufferHeight;
    private boolean visible;

    /** Creates a hidden overlay, optionally sharing the main OpenGL context. */
    public NativeWindow(boolean openGl) {
        mainWindow = Minecraft.getInstance().getWindow().handle();
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, openGl ? GLFW.GLFW_OPENGL_API : GLFW.GLFW_NO_API);
        if (openGl) {
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_NATIVE_CONTEXT_API);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
        }
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_MOUSE_PASSTHROUGH, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
        GLFW.glfwGetWindowSize(mainWindow, horizontal, vertical);
        long previousContext = openGl ? GLFW.glfwGetCurrentContext() : 0L;
        if (previousContext != 0L) GLFW.glfwMakeContextCurrent(0L);
        try {
            window = GLFW.glfwCreateWindow(horizontal[0], vertical[0], "", 0L, openGl ? mainWindow : 0L);
        } finally {
            if (previousContext != 0L) GLFW.glfwMakeContextCurrent(previousContext);
        }
        if (window == 0L) throw new IllegalStateException("Failed to create overlay window: " + lastGlfwError());
        configureCaptureSharing();
        attachToMainWindow();
        EventBus.register(this);
    }

    @Override
    public void synchronizeWithMainWindow() {
        GLFW.glfwGetWindowPos(mainWindow, horizontal, vertical);
        boolean moved = horizontal[0] != x || vertical[0] != y;
        x = horizontal[0];
        y = vertical[0];
        GLFW.glfwGetWindowSize(mainWindow, horizontal, vertical);
        if (horizontal[0] != width || vertical[0] != height) {
            width = horizontal[0];
            height = vertical[0];
            if (width > 0 && height > 0) GLFW.glfwSetWindowSize(window, width, height);
            moved = true;
        }
        if (moved) GLFW.glfwSetWindowPos(window, x, y);
        GLFW.glfwGetFramebufferSize(window, horizontal, vertical);
        framebufferWidth = horizontal[0];
        framebufferHeight = vertical[0];
    }

    @Override
    public void setVisible(boolean requested) {
        if (visible == requested) return;
        visible = requested;
        if (requested) show();
        else GLFW.glfwHideWindow(window);
    }

    @Override
    public void close() {
        EventBus.unregister(this);
        GLFW.glfwDestroyWindow(window);
    }

    @Subscribe
    public void onWindowPosition(WindowPositionEvent event) { synchronizeWithMainWindow(); }

    @Subscribe
    public void onWindowSize(WindowSizeEvent event) { synchronizeWithMainWindow(); }

    @Subscribe
    public void onWindowFocus(WindowFocusEvent event) {
        if (event.focused && visible) show();
    }

    private void show() {
        synchronizeWithMainWindow();
        GLFW.glfwShowWindow(window);
        attachToMainWindow();
    }

    private String lastGlfwError() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer description = stack.mallocPointer(1);
            int code = GLFW.glfwGetError(description);
            String text = description.get(0) == 0L ? "" : MemoryUtil.memUTF8(description.get(0));
            return code + " " + text;
        }
    }

    private void attachToMainWindow() {
        switch (Platform.get()) {
            case WINDOWS -> {
                long overlay = GLFWNativeWin32.glfwGetWin32Window(window);
                long owner = GLFWNativeWin32.glfwGetWin32Window(mainWindow);
                SharedLibrary user32 = APIUtil.apiCreateLibrary("user32");
                long setOwner = user32.getFunctionAddress("SetWindowLongPtrW");
                JNI.invokePPPP(overlay, OWNER_WINDOW_INDEX, owner, setOwner);
            }
            case MACOSX -> {
                long overlay = GLFWNativeCocoa.glfwGetCocoaWindow(window);
                long owner = GLFWNativeCocoa.glfwGetCocoaWindow(mainWindow);
                long sendMessage = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
                long addChild = ObjCRuntime.sel_getUid("addChildWindow:ordered:");
                JNI.invokePPPPV(owner, addChild, overlay, WINDOW_ABOVE, sendMessage);
            }
            default -> {
                // Original implementation has no native owner-attachment operation here.
                return;
            }
        }
    }

    private void configureCaptureSharing() {
        switch (Platform.get()) {
            case WINDOWS -> {
                long overlay = GLFWNativeWin32.glfwGetWin32Window(window);
                SharedLibrary user32 = APIUtil.apiCreateLibrary("user32");
                long setAffinity = user32.getFunctionAddress("SetWindowDisplayAffinity");
                JNI.invokePPI(overlay, EXCLUDE_FROM_CAPTURE, setAffinity);
            }
            case MACOSX -> {
                long overlay = GLFWNativeCocoa.glfwGetCocoaWindow(window);
                long sendMessage = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
                long setSharing = ObjCRuntime.sel_getUid("setSharingType:");
                JNI.invokePPPV(overlay, setSharing, SHARING_NONE, sendMessage);
            }
            default -> {
                // GLFW-only platforms retain their original capture-sharing limitation.
                return;
            }
        }
    }

    @Override public long windowHandle() { return window; }
    @Override public int framebufferWidth() { return framebufferWidth; }
    @Override public int framebufferHeight() { return framebufferHeight; }
}
