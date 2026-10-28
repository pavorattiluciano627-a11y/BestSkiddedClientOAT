package dev.echo.client.scripting;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/** Strict property reads used by the script definition parser. */
public final class ScriptProperties {
    private ScriptProperties() {
    }

    public static Function function(Scriptable object, String name) {
        if (object == null) {
            return null;
        }
        Object value = object.get(name, object);
        return value instanceof Function function ? function : null;
    }

    public static Object property(Scriptable object, Object id) {
        return id instanceof Number number
            ? object.get(number.intValue(), object)
            : object.get(String.valueOf(id), object);
    }

    public static Scriptable object(Scriptable object, String name) {
        Object value = object.get(name, object);
        return value instanceof Scriptable scriptable ? scriptable : null;
    }

    public static String string(Scriptable object, String name, String fallback) {
        Object value = object.get(name, object);
        return value instanceof String text ? text : fallback;
    }

    public static int integer(Scriptable object, String name, int fallback) {
        Object value = object.get(name, object);
        return value instanceof Number number ? number.intValue() : fallback;
    }
}
