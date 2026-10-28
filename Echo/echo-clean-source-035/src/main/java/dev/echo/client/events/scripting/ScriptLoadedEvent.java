package dev.echo.client.events.scripting;

import dev.echo.client.events.Event;
import dev.echo.client.scripting.ScriptFeature;

/** Loaded-script payload. Original OutputHandler identity; linkage remains conditional. */
public class ScriptLoadedEvent extends Event {
    private final ScriptFeature feature;
    private final String scriptName;

    /** Captures the newly registered feature and its script name after loading. */
    public ScriptLoadedEvent(ScriptFeature feature, String scriptName) {
        this.feature = feature;
        this.scriptName = scriptName;
    }

    /** Returns the loaded script feature. */
    public ScriptFeature getFeature() { return feature; }

    /** Returns the script name supplied by the manager. */
    public String getScriptName() { return scriptName; }
}
