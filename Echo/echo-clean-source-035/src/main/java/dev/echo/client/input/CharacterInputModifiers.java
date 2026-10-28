package dev.echo.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Extracts the GLFW modifier mask for a character-input event.
 *
 * 26.2 stopped carrying a modifier mask on {@link CharacterEvent}, so the mask
 * is read from live keyboard state at the moment the character arrives. The
 * returned value keeps the GLFW bit layout the screens already test against.
 */
public final class CharacterInputModifiers {
    private CharacterInputModifiers() {
    }

    public static int modifiers(CharacterEvent input) {
        Window window = Minecraft.getInstance().getWindow();
        if (window == null) {
            return 0;
        }
        int mask = 0;
        if (down(window, GLFW.GLFW_KEY_LEFT_SHIFT) || down(window, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            mask |= GLFW.GLFW_MOD_SHIFT;
        }
        if (down(window, GLFW.GLFW_KEY_LEFT_CONTROL) || down(window, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            mask |= GLFW.GLFW_MOD_CONTROL;
        }
        if (down(window, GLFW.GLFW_KEY_LEFT_ALT) || down(window, GLFW.GLFW_KEY_RIGHT_ALT)) {
            mask |= GLFW.GLFW_MOD_ALT;
        }
        if (down(window, GLFW.GLFW_KEY_LEFT_SUPER) || down(window, GLFW.GLFW_KEY_RIGHT_SUPER)) {
            mask |= GLFW.GLFW_MOD_SUPER;
        }
        return mask;
    }

    private static boolean down(Window window, int key) {
        return InputConstants.isKeyDown(window, key);
    }
}
