package dev.echo.client.scripting;

import java.util.function.BiConsumer;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Complete execution portion of the script engine, separated from application API
 * registration and diagnostic presentation. JavaScript source, source names,
 * scopes, receivers, arguments, and functions are passed through without filtering.
 *
 * <p>The application prepares its shared API scope once and supplies the operation
 * that reports a load-time evaluation exception. Compilation failures and callback
 * failures deliberately propagate to their callers.</p>
 */
public final class RhinoScriptRuntime {
    private final ContextFactory contextFactory;
    private final ScriptableObject sharedScope;
    private final BiConsumer<String, Exception> reportEvaluationFailure;

    /**
     * Creates an execution runtime with Echo's ES6 context factory.
     *
     * @param sharedScope the initialized standard-object and application API scope
     * @param reportEvaluationFailure the filename/exception reporter for script evaluation
     */
    public RhinoScriptRuntime(ScriptableObject sharedScope,
                              BiConsumer<String, Exception> reportEvaluationFailure) {
        this(new ScriptContextFactory(), sharedScope, reportEvaluationFailure);
    }

    /**
     * Uses the same context factory that initialized the application API scope.
     *
     * @param contextFactory the engine's existing context factory
     * @param sharedScope the initialized shared scope
     * @param reportEvaluationFailure the filename/exception evaluation reporter
     */
    public RhinoScriptRuntime(ContextFactory contextFactory, ScriptableObject sharedScope,
                              BiConsumer<String, Exception> reportEvaluationFailure) {
        this.contextFactory = contextFactory;
        this.sharedScope = sharedScope;
        this.reportEvaluationFailure = reportEvaluationFailure;
    }

    /**
     * Compiles and evaluates a script in a new child scope.
     *
     * <p>Compilation precedes scope creation and the evaluation exception boundary.
     * A successful evaluation returns the new scope, not the script's last value.
     * Scope creation failures, {@link Error}s, and reporter failures propagate.</p>
     *
     * @param source the complete JavaScript source
     * @param sourceName the diagnostic source name
     * @return the per-script scope, or null after a reported evaluation exception
     */
    public Scriptable run(String source, String sourceName) {
        Script script = compile(source, sourceName);
        return contextFactory.call(context -> {
            Scriptable scope = RhinoScopes.createScriptScope(context, sharedScope);
            return RhinoOperations.executeInScope(context, script, scope,
                    exception -> reportEvaluationFailure.accept(sourceName, exception));
        });
    }

    /**
     * Calls a script callback with its lexical scope and receiver kept distinct.
     * No exception is caught here; event and lifecycle callers own that boundary.
     *
     * @param function the callback
     * @param scope the lexical scope
     * @param thisObject the JavaScript receiver
     * @param arguments the original argument array
     * @return the unmodified Rhino result
     */
    public Object call(Function function, Scriptable scope, Scriptable thisObject, Object[] arguments) {
        return contextFactory.call(context -> RhinoOperations.call(context, function, scope, thisObject, arguments));
    }

    private Script compile(String source, String sourceName) {
        return contextFactory.call(context -> RhinoOperations.compile(context, source, sourceName));
    }
}
