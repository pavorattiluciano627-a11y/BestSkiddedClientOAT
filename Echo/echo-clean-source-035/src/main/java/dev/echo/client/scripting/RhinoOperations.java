package dev.echo.client.scripting;

import java.util.function.Consumer;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

/** Direct Rhino operations split from the engine's compiler-generated callbacks. */
public final class RhinoOperations {
    private RhinoOperations() {
    }

    /**
     * Compiles source at line one, with no security-domain object.
     *
     * @param context the context supplied by the engine's ContextFactory.call
     * @param source the JavaScript source
     * @param sourceName the diagnostic source name
     * @return the compiled script
     */
    public static Script compile(Context context, String source, String sourceName) {
        return context.compileString(source, sourceName, 1, null);
    }

    /**
     * Calls a function without coercing its arguments or intercepting its exception.
     * The lexical scope and JavaScript {@code this} object deliberately remain distinct.
     *
     * @param context the active Rhino context
     * @param function the JavaScript callback
     * @param scope the lexical scope
     * @param thisObject the callback receiver
     * @param arguments the original argument array
     * @return the unmodified Rhino result
     */
    public static Object call(Context context, Function function, Scriptable scope,
                              Scriptable thisObject, Object[] arguments) {
        return function.call(context, scope, thisObject, arguments);
    }

    /**
     * Executes a compiled script in a prepared scope and reports evaluation exceptions.
     * Compilation and scope creation belong outside this exception boundary. Errors
     * are not caught, and an exception from the reporter itself propagates.
     *
     * @param context the active context
     * @param script the compiled script
     * @param scope the prepared per-script scope
     * @param reportFailure the original engine's error-report operation, bound by its caller
     * @return the same scope, or null after a reported evaluation exception
     */
    public static Scriptable executeInScope(Context context, Script script, Scriptable scope,
                                            Consumer<Exception> reportFailure) {
        try {
            script.exec(context, scope);
            return scope;
        } catch (Exception exception) {
            reportFailure.accept(exception);
            return null;
        }
    }
}
