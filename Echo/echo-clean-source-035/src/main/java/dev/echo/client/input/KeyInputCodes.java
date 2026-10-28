package dev.echo.client.input;

import net.minecraft.client.input.KeyEvent;

/** Reads the physical scan code used by the original keyboard adapter. */
public final class KeyInputCodes {
    private KeyInputCodes() {
        throw new UnsupportedOperationException(
                "This is a utility class and cannot be instantiated");
    }

    /**
     * Returns the scan code, rather than the logical GLFW key value.
     *
     * @param input the Minecraft keyboard event
     * @return the event's physical scan code
     */
    public static int scanCode(KeyEvent input) {
        return input.scancode();
    }
}
