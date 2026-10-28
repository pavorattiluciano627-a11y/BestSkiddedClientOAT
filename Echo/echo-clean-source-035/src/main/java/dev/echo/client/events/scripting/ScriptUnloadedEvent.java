package dev.echo.client.events.scripting;

import dev.echo.client.events.Event;
import dev.echo.client.scripting.ScriptFeature;

/** Unloaded-script payload. Original ModuleHandler identity; linkage remains conditional. */
public class ScriptUnloadedEvent extends Event {
    private final ScriptFeature feature;
    private final String scriptName;

    /** Captures the removed feature and its script name after unloading. */
    public ScriptUnloadedEvent(ScriptFeature feature, String scriptName) {
        this.feature = feature;
        this.scriptName = scriptName;
    }

    /** Returns the unloaded script feature. */
    public ScriptFeature getFeature() { return feature; }

    /** Returns the script name supplied by the manager. */
    public String getScriptName() { return scriptName; }
}
