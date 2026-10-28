package dev.echo.client.scripting;

/** Strict argument extraction used by script setting factories. */
public final class SettingArguments {
    private SettingArguments() {
    }

    public static String string(Object[] arguments, int index, String fallback) {
        return arguments.length > index && arguments[index] instanceof String text ? text : fallback;
    }

    public static int integer(Object[] arguments, int index, int fallback) {
        return arguments.length > index && arguments[index] instanceof Number number ? number.intValue() : fallback;
    }

    public static float floating(Object[] arguments, int index, float fallback) {
        return arguments.length > index && arguments[index] instanceof Number number ? number.floatValue() : fallback;
    }
}
