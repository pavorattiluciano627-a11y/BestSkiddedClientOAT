package dev.echo.client.input;

/**
 * Original GLFW and native-hook modifier masks.
 * Source-complete candidate: mask tests retain the Key opaque-state/cache applicability obligation.
 */
public enum KeyModifier {
    SHIFT(1, 3),
    CONTROL(2, 192),
    ALT(4, 768),
    SUPER(8, 3072);

    private final int glfwMask;
    private final int nativeMask;

    KeyModifier(int glfwMask, int nativeMask) {
        this.glfwMask = glfwMask;
        this.nativeMask = nativeMask;
    }

    /** Returns the GLFW modifier bit used by UI key events. */
    public int mask() {
        return glfwMask;
    }

    /** Tests this modifier against an event's modifier bits. */
    public boolean isSet(int modifiers) {
        return (modifiers & mask()) != 0;
    }
}
