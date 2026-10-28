package dev.echo.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.echo.client.mixin.KeyBindingAccessor;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Readable input-state consumer candidate from the fixed-native-v2 fragments.
 * Promotion requires the per-method binding and active-state obligations in
 * fixed-consumers/contract.json; this class is intentionally analysis-only.
 */
public final class KeybindingCache {
    public static final int RELEASE = 0;
    public static final int PRESS = 1;

    private static final ConcurrentHashMap<Integer, Boolean> overrides = new ConcurrentHashMap<>();
    private static final boolean[] mousePressed = new boolean[8];

    private KeybindingCache() {
    }

    /** Keep only overrides that differ from the unmodified input state. */
    public static void updateOverride(int code, boolean pressed, boolean originalPressed) {
        if (pressed != originalPressed) {
            overrides.put(code, pressed);
        } else {
            overrides.remove(code);
        }
    }

    /** Null means that no override exists; false is a meaningful override. */
    public static Boolean getOverride(int code) {
        return overrides.get(code);
    }

    public static void clearOverrides() {
        overrides.clear();
    }

    public static void setMousePressed(int button, boolean pressed) {
        if (button >= 0 && button < mousePressed.length) {
            mousePressed[button] = pressed;
        }
    }

    public static boolean isMousePressed(int button) {
        if (button < 0 || button >= mousePressed.length) {
            return false;
        }
        if (!Minecraft.getInstance().isWindowActive()) {
            Arrays.fill(mousePressed, false);
            return false;
        }
        return mousePressed[button];
    }

    public static InputKey getKey(KeyMapping binding) {
        if (binding == null) {
            return InputKey.NONE;
        }
        InputConstants.Key boundKey = ((KeyBindingAccessor) binding).getBoundKey();
        if (boundKey == null) {
            return InputKey.NONE;
        }
        return boundKey.getType() == InputConstants.Type.MOUSE
                ? InputKey.fromMouseCode(boundKey.getValue())
                : InputKey.fromCode(boundKey.getValue());
    }

    public static boolean isPressed(KeyMapping binding) {
        ensureInitialized(KeyMapping.class);
        ensureInitialized(InputKey.class);
        return getKey(binding).isPressed();
    }

    /** Preserve the fixed caller's class-initialization order, even for null input. */
    private static void ensureInitialized(Class<?> type) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Resolved class initialization failed", exception);
        }
    }
}
