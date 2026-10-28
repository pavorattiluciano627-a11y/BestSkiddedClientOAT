package dev.echo.client.input;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

/**
 * Structured candidate for temporary key-state overrides.
 * Pending: direct target proofs, the activeFile guard invariant, and Key/KeyBindings API merge.
 */
public final class KeyStateOverrides {
    private static final long OVERRIDE_LIFETIME_NANOS = 75_000_000L;
    private static final Map<KeyMapping, KeyOverride> overrides = new ConcurrentHashMap<>();
    private static final String BOUND_KEY_BRIDGE_METHOD = "bridge$getKey";
    private static Method boundKeyBridge;

    private KeyStateOverrides() {
    }

    /** Replaces the binding's override for seventy-five milliseconds. */
    public static void setPressed(KeyMapping binding, boolean pressed) {
        overrides.put(binding, new KeyOverride(pressed, System.nanoTime() + OVERRIDE_LIFETIME_NANOS));
    }

    /** Returns a live override, falling back to the binding's actual state. */
    public static boolean isPressed(KeyMapping binding) {
        Boolean override = activeOverride(binding);
        return override == null ? binding.isDown() : override;
    }

    /**
     * Returns true for a pressed matching attack/use mouse binding, otherwise null.
     * The original does not return false for an unpressed matching binding.
     */
    public static Boolean pressedActionForMouseButton(int code) {
        Options options = Minecraft.getInstance().options;
        if (options == null) {
            return null;
        }
        InputKey key = InputKey.fromMouseCode(code);
        for (KeyMapping binding : new KeyMapping[]{options.keyAttack, options.keyUse}) {
            if (KeybindingCache.getKey(binding) == key && isPressed(binding)) {
                return true;
            }
        }
        return null;
    }

    /** Looks up a live override by the identity of a bound input key. */
    public static Boolean forBoundKey(Object key) {
        for (KeyMapping binding : overrides.keySet()) {
            if (readBoundKey(binding) == key) {
                return activeOverride(binding);
            }
        }
        return null;
    }

    private static Boolean activeOverride(KeyMapping binding) {
        KeyOverride override = overrides.get(binding);
        if (override == null) {
            return null;
        }
        if (override.expiresAtNanos() >= System.nanoTime()) {
            return override.pressed();
        }
        overrides.remove(binding, override);
        return null;
    }

    private static Object readBoundKey(KeyMapping binding) {
        try {
            if (boundKeyBridge == null) {
                boundKeyBridge = binding.getClass().getMethod(BOUND_KEY_BRIDGE_METHOD);
            }
            return boundKeyBridge.invoke(binding);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }
}
