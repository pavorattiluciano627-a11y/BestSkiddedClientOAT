package dev.echo.client.input;

import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;

/** Restores the normal cursor mode at the requested window coordinates. */
public final class CursorControls {
    private CursorControls() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static void showCursor(Window window, double x, double y) {
        InputUtil.setCursorParameters(window, GLFW.GLFW_CURSOR_NORMAL, x, y);
    }
}
