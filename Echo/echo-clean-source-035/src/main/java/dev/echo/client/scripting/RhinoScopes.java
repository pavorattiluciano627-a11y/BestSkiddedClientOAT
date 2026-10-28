package dev.echo.client.scripting;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/** Rhino scope operations extracted from the script engine. */
public final class RhinoScopes {
    private RhinoScopes() {
    }

    /**
     * Creates a fresh scope inheriting the shared scope, with no parent scope.
     *
     * @param context the active context
     * @param sharedScope the standard-object and API scope
     * @return the new per-script scope
     */
    public static Scriptable createScriptScope(Context context, Scriptable sharedScope) {
        Scriptable scope = context.newObject(sharedScope);
        scope.setPrototype(sharedScope);
        scope.setParentScope(null);
        return scope;
    }

    /**
     * Defines an API class, constructs its instance, and publishes that instance.
     * Registration errors are printed and swallowed independently for each API.
     *
     * @param context the active context
     * @param sharedScope the scope receiving the definition
     * @param apiClass the Rhino host class
     * @param className the class name returned by the host's getClassName
     */
    public static void defineApi(Context context, ScriptableObject sharedScope,
                                 Class<? extends Scriptable> apiClass, String className) {
        try {
            ScriptableObject.defineClass(sharedScope, apiClass);
            Scriptable instance = context.newObject(sharedScope, className);
            ScriptableObject.putProperty(sharedScope, className, instance);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
