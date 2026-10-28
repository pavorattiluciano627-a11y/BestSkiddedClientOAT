package dev.echo.client.scripting.api;

import dev.echo.client.scripting.ConsoleMessages;
import dev.echo.client.scripting.ScriptEngine;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSFunction;

/** Script console; calls deliberately do not depend on the JavaScript receiver. */
public class ConsoleApi extends ScriptableObject {
    @Override
    public String getClassName() {
        return "console";
    }

    @JSFunction("decodeView")
    public static void info(Context context, Scriptable self, Object[] arguments, Function function) {
        ConsoleMessages.info(ScriptEngine.LOGGER, arguments);
    }

    @JSFunction("validateState")
    public static void warn(Context context, Scriptable self, Object[] arguments, Function function) {
        ConsoleMessages.warn(ScriptEngine.LOGGER, arguments);
    }

    @JSFunction("parseTask")
    public static void error(Context context, Scriptable self, Object[] arguments, Function function) {
        ConsoleMessages.error(ScriptEngine.LOGGER, arguments);
    }

    @JSFunction("hasTemplate")
    public static void debug(Context context, Scriptable self, Object[] arguments, Function function) {
        ConsoleMessages.debug(ScriptEngine.LOGGER, arguments);
    }
}
