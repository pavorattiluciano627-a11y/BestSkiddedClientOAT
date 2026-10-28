package dev.echo.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;

/** Restores the normal cursor mode at the requested window coordinates. */
public final class CursorControls {
    private CursorControls() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static void showCursor(Window window, double x, double y) {
        InputConstants.grabOrReleaseMouse(window, GLFW.GLFW_CURSOR_NORMAL, x, y);
    }
}
