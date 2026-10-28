package dev.echo.compat.lwjgl;

import org.lwjgl.util.tinyfd.TinyFileDialogs;

/** Restores the boolean Java contract of LWJGL 3.3.3's message box overload. */
public final class LegacyTinyFileDialogs {
    private LegacyTinyFileDialogs() {}

    /** Preserves the strings, default-button conversion, and nonzero result rule. */
    public static boolean messageBox(CharSequence title, CharSequence message,
            CharSequence dialogType, CharSequence iconType, boolean defaultButton) {
        return messageBox(title, message, dialogType, iconType, defaultButton,
                TinyFileDialogs::tinyfd_messageBox);
    }

    /** Recording seam for dialog-free tests; the production boundary is LWJGL. */
    static boolean messageBox(CharSequence title, CharSequence message,
            CharSequence dialogType, CharSequence iconType, boolean defaultButton,
            MessageBoxCall call) {
        return call.show(title, message, dialogType, iconType, defaultButton ? 1 : 0) != 0;
    }

    /** The integer-returning LWJGL 3.4.3 boundary. */
    @FunctionalInterface
    interface MessageBoxCall {
        int show(CharSequence title, CharSequence message, CharSequence dialogType,
                CharSequence iconType, int defaultButton);
    }
}
