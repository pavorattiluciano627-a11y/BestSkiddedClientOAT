package dev.echo.client.scripting;

import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import org.mozilla.javascript.Function;

/** Complete structured lifecycle candidate, including both recovered native bodies. */
public class ScriptFeature extends Feature {
    private final ScriptDefinition definition;
    private final ScriptEngine engine;
    private final ScriptCallbacks callbacks;

    /** Captures the definition and engine without copying the definition's collections. */
    public ScriptFeature(ScriptDefinition definition, ScriptEngine engine) {
        super(new FeatureInfo(definition.name(), definition.description(), definition.category(), definition.key()));
        this.definition = definition;
        this.engine = engine;
        callbacks = new ScriptCallbacks(engine, definition);
    }

    /** Uses the script's declared settings instead of reflection over Java fields. */
    @Override
    public void initSettings() {
        settings.clear();
        settings.addAll(definition.settings());
    }

    /** Runs the base enable hook, registers callbacks, then executes the script hook. */
    @Override
    public void onEnable() {
        super.onEnable();
        callbacks.register();
        invokeHook("enable", definition.onEnable());
    }

    /** Executes the script hook, unregisters callbacks, then runs the base disable hook. */
    @Override
    public void onDisable() {
        invokeHook("disable", definition.onDisable());
        callbacks.unregister();
        super.onDisable();
    }

    /** Returns the definition's script identifier, originally its filename. */
    public String getScriptId() { return definition.id(); }

    private void invokeHook(String name, Function hook) {
        if (hook == null) return;
        try {
            engine.call(hook, definition.scope(), definition.scriptObject(), new Object[0]);
        } catch (Exception exception) {
            ScriptErrorReporter.report(ScriptEngine.LOGGER, definition.id(), "'" + name + "' hook", exception);
        }
    }
}
