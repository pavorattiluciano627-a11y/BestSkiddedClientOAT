package dev.echo.client.scripting;

import org.mozilla.javascript.Undefined;

/** The client's coercion rule, which differs from JavaScript for NaN. */
public final class ScriptValues {
    private ScriptValues() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static boolean toBoolean(Object value) {
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value instanceof Number number) {
            return number.doubleValue() != 0.0;
        }
        if (value instanceof String text) {
            return !text.isEmpty();
        }
        return value != null && value != Undefined.instance;
    }
}
