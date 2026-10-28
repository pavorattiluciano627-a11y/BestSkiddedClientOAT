package dev.echo.client.input;

import net.minecraft.client.input.CharInput;

/** Extracts the GLFW modifier mask carried by a character-input event. */
public final class CharacterInputModifiers {
    private CharacterInputModifiers() {
    }

    public static int modifiers(CharInput input) {
        return input.modifiers();
    }
}
