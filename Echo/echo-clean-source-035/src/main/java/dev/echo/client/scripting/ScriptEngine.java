package dev.echo.client.scripting;

import dev.echo.client.scripting.api.ChatApi;
import dev.echo.client.scripting.api.ConsoleApi;
import dev.echo.client.scripting.api.MinecraftApi;
import dev.echo.client.scripting.api.SettingApi;
import dev.echo.client.scripting.api.SettingsApi;
import java.util.Set;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Complete structured engine candidate under the intended binding domain.
 * Application API/error classes belong to other source units. Shared-cache and
 * opaque-state prerequisites remain explicit in analysis/fixed-v2.
 */
public class ScriptEngine {
    public static final Logger LOGGER = LoggerFactory.getLogger("echo/script");

    // This predicate was present but was not installed as a Rhino ClassShutter.
    // Its original compatibility strings are literal data, not hidden Java dispatch.
    private static final String[] LISTED_PACKAGE_PREFIXES = {
        "hack.echo.client.script.api.", "hack.echo.client.event.impl.",
        "hack.echo.client.draw2d.", "hack.echo.client.draw3d."
    };
    private static final Set<String> LISTED_CLASSES = Set.of(
        "java.lang.Object", "java.lang.String", "java.lang.CharSequence", "java.lang.Number",
        "java.lang.Boolean", "java.lang.Character", "java.lang.Byte", "java.lang.Short",
        "java.lang.Integer", "java.lang.Long", "java.lang.Float", "java.lang.Double"
    );

    private final RhinoScriptRuntime runtime;

    /** Defines APIs in original order and preserves the shared minecraft/mc instance. */
    public ScriptEngine() {
        ScriptContextFactory factory = new ScriptContextFactory();
        ScriptableObject scope = factory.call(context -> {
            ScriptableObject shared = context.initStandardObjects();
            RhinoScopes.defineApi(context, shared, ConsoleApi.class, "console");
            RhinoScopes.defineApi(context, shared, SettingsApi.class, "settings");
            FontsRegistrationCompatibility.reportFailure();
            RhinoScopes.defineApi(context, shared, ChatApi.class, "chat");
            RhinoScopes.defineApi(context, shared, MinecraftApi.class, "minecraft");
            ScriptableObject.putProperty(shared, "mc", ScriptableObject.getProperty(shared, "minecraft"));
            SettingApi.register(shared);
            return shared;
        });
        runtime = new RhinoScriptRuntime(factory, scope,
                (sourceName, exception) -> ScriptErrorReporter.report(LOGGER, sourceName, "threw during load", exception));
    }

    /** Compiles and evaluates source; the source and source name are semantic data. */
    public Scriptable run(String source, String sourceName) { return runtime.run(source, sourceName); }

    /** Passes callback arguments and the distinct lexical scope/receiver to Rhino. */
    public Object call(Function function, Scriptable scope, Scriptable receiver, Object[] arguments) {
        return runtime.call(function, scope, receiver, arguments);
    }

    private static boolean isListedJavaClass(String className) {
        if (LISTED_CLASSES.contains(className)) return true;
        for (String prefix : LISTED_PACKAGE_PREFIXES) {
            if (className.startsWith(prefix)) return true;
        }
        return false;
    }
}
