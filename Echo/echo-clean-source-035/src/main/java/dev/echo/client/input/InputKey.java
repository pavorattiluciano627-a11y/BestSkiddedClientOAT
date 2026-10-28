package dev.echo.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Original key data and readable fixed-checkpoint input consumers. This is an
 * analysis candidate: display-name parsing and the binding/state prerequisites
 * listed in fixed-consumers/contract.json remain outside this source closure.
 */
public enum InputKey {
    SPACE(new String("SPACE"), 32, 44),
    APOSTROPHE(new String("APOSTROPHE"), 39, 52),
    COMMA(new String("COMMA"), 44, 54),
    MINUS(new String("MINUS"), 45, 45),
    PERIOD(new String("PERIOD"), 46, 55),
    SLASH(new String("SLASH"), 47, 56),
    NUM_0(new String("NUM_0"), 48, 39),
    NUM_1(new String("NUM_1"), 49, 30),
    NUM_2(new String("NUM_2"), 50, 31),
    NUM_3(new String("NUM_3"), 51, 32),
    NUM_4(new String("NUM_4"), 52, 33),
    NUM_5(new String("NUM_5"), 53, 34),
    NUM_6(new String("NUM_6"), 54, 35),
    NUM_7(new String("NUM_7"), 55, 36),
    NUM_8(new String("NUM_8"), 56, 37),
    NUM_9(new String("NUM_9"), 57, 38),
    SEMICOLON(new String("SEMICOLON"), 59, 51),
    EQUAL(new String("EQUAL"), 61, 46),
    A("A", 65, 4),
    B("B", 66, 5),
    C("C", 67, 6),
    D("D", 68, 7),
    E("E", 69, 8),
    F("F", 70, 9),
    G("G", 71, 10),
    H("H", 72, 11),
    I("I", 73, 12),
    J("J", 74, 13),
    K("K", 75, 14),
    L("L", 76, 15),
    M("M", 77, 16),
    N("N", 78, 17),
    O("O", 79, 18),
    P("P", 80, 19),
    Q("Q", 81, 20),
    R("R", 82, 21),
    S("S", 83, 22),
    T("T", 84, 23),
    U("U", 85, 24),
    V("V", 86, 25),
    W("W", 87, 26),
    X("X", 88, 27),
    Y("Y", 89, 28),
    Z("Z", 90, 29),
    LEFT_BRACKET(new String("LEFT_BRACKET"), 91, 47),
    BACKSLASH(new String("BACKSLASH"), 92, 49),
    RIGHT_BRACKET(new String("RIGHT_BRACKET"), 93, 48),
    GRAVE_ACCENT(new String("GRAVE_ACCENT"), 96, 53),
    ESCAPE(new String("ESCAPE"), 256, 41),
    ENTER(new String("ENTER"), 257, 40),
    TAB(new String("TAB"), 258, 43),
    BACKSPACE(new String("BACKSPACE"), 259, 42),
    INSERT(new String("INSERT"), 260, 73),
    DELETE(new String("DELETE"), 261, 76),
    RIGHT(new String("RIGHT"), 262, 79),
    LEFT(new String("LEFT"), 263, 80),
    DOWN(new String("DOWN"), 264, 81),
    UP(new String("UP"), 265, 82),
    PAGE_UP(new String("PAGE_UP"), 266, 75),
    PAGE_DOWN(new String("PAGE_DOWN"), 267, 78),
    HOME(new String("HOME"), 268, 74),
    END(new String("END"), 269, 77),
    CAPS_LOCK(new String("CAPS_LOCK"), 280, 57),
    SCROLL_LOCK(new String("SCROLL_LOCK"), 281, 71),
    NUM_LOCK(new String("NUM_LOCK"), 282, 83),
    PRINT_SCREEN(new String("PRINT_SCREEN"), 283, 70),
    PAUSE(new String("PAUSE"), 284, 72),
    F1(new String("F1"), 290, 58),
    F2(new String("F2"), 291, 59),
    F3(new String("F3"), 292, 60),
    F4(new String("F4"), 293, 61),
    F5(new String("F5"), 294, 62),
    F6(new String("F6"), 295, 63),
    F7(new String("F7"), 296, 64),
    F8(new String("F8"), 297, 65),
    F9(new String("F9"), 298, 66),
    F10(new String("F10"), 299, 67),
    F11(new String("F11"), 300, 68),
    F12(new String("F12"), 301, 69),
    F13(new String("F13"), 302, 104),
    F14(new String("F14"), 303, 105),
    F15(new String("F15"), 304, 106),
    F16(new String("F16"), 305, 107),
    F17(new String("F17"), 306, 108),
    F18(new String("F18"), 307, 109),
    F19(new String("F19"), 308, 110),
    F20(new String("F20"), 309, 111),
    F21(new String("F21"), 310, 112),
    F22(new String("F22"), 311, 113),
    F23(new String("F23"), 312, 114),
    F24(new String("F24"), 313, 115),
    KEYPAD_0(new String("KEYPAD_0"), 320, 98),
    KEYPAD_1(new String("KEYPAD_1"), 321, 89),
    KEYPAD_2(new String("KEYPAD_2"), 322, 90),
    KEYPAD_3(new String("KEYPAD_3"), 323, 91),
    KEYPAD_4(new String("KEYPAD_4"), 324, 92),
    KEYPAD_5(new String("KEYPAD_5"), 325, 93),
    KEYPAD_6(new String("KEYPAD_6"), 326, 94),
    KEYPAD_7(new String("KEYPAD_7"), 327, 95),
    KEYPAD_8(new String("KEYPAD_8"), 328, 96),
    KEYPAD_9(new String("KEYPAD_9"), 329, 97),
    KEYPAD_DECIMAL(new String("KEYPAD_DECIMAL"), 330, 99),
    KEYPAD_DIVIDE(new String("KEYPAD_DIVIDE"), 331, 84),
    KEYPAD_MULTIPLY(new String("KEYPAD_MULTIPLY"), 332, 85),
    KEYPAD_SUBTRACT(new String("KEYPAD_SUBTRACT"), 333, 86),
    KEYPAD_ADD(new String("KEYPAD_ADD"), 334, 87),
    KEYPAD_ENTER(new String("KEYPAD_ENTER"), 335, 88),
    KEYPAD_EQUAL(new String("KEYPAD_EQUAL"), 336, 103),
    LEFT_SHIFT(new String("LEFT_SHIFT"), 340, 225),
    LEFT_CONTROL(new String("LEFT_CONTROL"), 341, 224),
    LEFT_ALT(new String("LEFT_ALT"), 342, 226),
    LEFT_SUPER(new String("LEFT_SUPER"), 343, 227),
    RIGHT_SHIFT(new String("RIGHT_SHIFT"), 344, 229),
    RIGHT_CONTROL(new String("RIGHT_CONTROL"), 345, 228),
    RIGHT_ALT(new String("RIGHT_ALT"), 346, 230),
    RIGHT_SUPER(new String("RIGHT_SUPER"), 347, 231),
    MOUSE_LEFT(new String("MOUSE_LEFT"), 0, 1, true),
    MOUSE_RIGHT(new String("MOUSE_RIGHT"), 1, 3, true),
    MOUSE_MIDDLE(new String("MOUSE_MIDDLE"), 2, 2, true),
    MOUSE_4(new String("MOUSE_4"), 3, 4, true),
    MOUSE_5(new String("MOUSE_5"), 4, 5, true),
    MOUSE_6(new String("MOUSE_6"), 5, 6, true),
    MOUSE_7(new String("MOUSE_7"), 6, 7, true),
    MOUSE_8(new String("MOUSE_8"), 7, 8, true),
    NONE(new String("NONE"), -1, -1);

    private static final Int2ObjectArrayMap<InputKey> keyboardByCode = new Int2ObjectArrayMap<>();
    private static final Int2ObjectArrayMap<InputKey> mouseByCode = new Int2ObjectArrayMap<>();

    private final String label;
    private final int glfwCode;
    private final int legacyScanCode;
    private final boolean mouse;
    private int code;
    private static final String DISPLAY_NONE = NONE.label;
    private static final String DISPLAY_LEFT_MOUSE = new String("LMB");
    private static final String DISPLAY_RIGHT_MOUSE = new String("RMB");
    private static final String DISPLAY_MIDDLE_MOUSE = new String("MMB");
    private static final String DISPLAY_MOUSE_PREFIX = new String("MB");
    private static final String DISPLAY_LEFT = new String("LEFT ");
    private static final String DISPLAY_LEFT_SHORT = new String("L ");
    private static final String DISPLAY_RIGHT = new String("RIGHT ");
    private static final String DISPLAY_RIGHT_SHORT = new String("R ");
    private static final String DISPLAY_CONTROL = new String("CONTROL");
    private static final String DISPLAY_CONTROL_SHORT = new String("CTRL");
    private static final String DISPLAY_BACKSPACE = BACKSPACE.label;
    private static final String DISPLAY_BACKSPACE_SHORT = new String("BKSP");
    private static final String DISPLAY_DELETE = DELETE.label;
    private static final String DISPLAY_DELETE_SHORT = new String("DEL");
    private static final String DISPLAY_INSERT = INSERT.label;
    private static final String DISPLAY_INSERT_SHORT = new String("INS");
    private static final String DISPLAY_ESCAPE = ESCAPE.label;
    private static final String DISPLAY_ESCAPE_SHORT = new String("ESC");
    private static final String DISPLAY_PAGE_UP = new String("PAGE UP");
    private static final String DISPLAY_PAGE_UP_SHORT = new String("PG UP");
    private static final String DISPLAY_PAGE_DOWN = new String("PAGE DOWN");
    private static final String DISPLAY_PAGE_DOWN_SHORT = new String("PG DN");

    InputKey(String label, int glfwCode, int legacyScanCode) {
        this(label, glfwCode, legacyScanCode, false);
    }

    InputKey(String label, int glfwCode, int legacyScanCode, boolean mouse) {
        this.label = label;
        this.glfwCode = glfwCode;
        this.legacyScanCode = legacyScanCode;
        this.mouse = mouse;
    }

    static {
        for (InputKey key : values()) {
            key.code = key.glfwCode;
            if (key != NONE) {
                (key.mouse ? mouseByCode : keyboardByCode).put(key.code, key);
            }
        }
    }

    @Override
    public String toString() {
        return label;
    }

    /** Original localized display operation at the recorded UI caller tuple. */
    public String displayLabel() {
        if (this == NONE) return DISPLAY_NONE;
        if (mouse) {
            return switch (this) {
                case MOUSE_LEFT -> DISPLAY_LEFT_MOUSE;
                case MOUSE_RIGHT -> DISPLAY_RIGHT_MOUSE;
                case MOUSE_MIDDLE -> DISPLAY_MIDDLE_MOUSE;
                default -> DISPLAY_MOUSE_PREFIX + (ordinal() - MOUSE_LEFT.ordinal() + 1);
            };
        }
        return normalizeDisplayLabel(InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString());
    }

    private static String normalizeDisplayLabel(String text) {
        // Default-locale casing and replacement order are original behavior.
        return text.toUpperCase()
                .replace(DISPLAY_LEFT, DISPLAY_LEFT_SHORT)
                .replace(DISPLAY_RIGHT, DISPLAY_RIGHT_SHORT)
                .replace(DISPLAY_CONTROL, DISPLAY_CONTROL_SHORT)
                .replace(DISPLAY_BACKSPACE, DISPLAY_BACKSPACE_SHORT)
                .replace(DISPLAY_DELETE, DISPLAY_DELETE_SHORT)
                .replace(DISPLAY_INSERT, DISPLAY_INSERT_SHORT)
                .replace(DISPLAY_ESCAPE, DISPLAY_ESCAPE_SHORT)
                .replace(DISPLAY_PAGE_UP, DISPLAY_PAGE_UP_SHORT)
                .replace(DISPLAY_PAGE_DOWN, DISPLAY_PAGE_DOWN_SHORT);
    }

    public int code() {
        return code;
    }

    public boolean isMouse() {
        return mouse;
    }

    public boolean matches(int inputCode) {
        return code == inputCode;
    }

    public static InputKey fromCode(int code) {
        return keyboardByCode.getOrDefault(code, NONE);
    }

    public static InputKey fromMouseCode(int code) {
        return mouseByCode.getOrDefault(code, NONE);
    }

    /**
     * Finds the first original label equal to the supplied String ignoring case.
     * Input is not trimmed, renamed or interned. See parse-contract.json for
     * the original binding, null-sentinel and packed-parameter preconditions.
     */
    public static InputKey parse(String text) {
        if (text == null || text.isBlank()) {
            return NONE;
        }
        for (InputKey key : values()) {
            if (key.label.equalsIgnoreCase(text)) {
                return key;
            }
        }
        return NONE;
    }

    public boolean isPressed() {
        if (this == NONE) {
            return false;
        }
        long window = Minecraft.getInstance().getWindow().handle();
        if (mouse) {
            return GLFW.glfwGetMouseButton(window, code) == GLFW.GLFW_PRESS
                    || KeybindingCache.isMousePressed(code);
        }
        return GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
    }
}
