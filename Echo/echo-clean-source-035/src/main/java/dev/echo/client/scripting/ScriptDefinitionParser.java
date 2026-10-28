package dev.echo.client.scripting;

import dev.echo.client.features.Category;
import dev.echo.client.input.InputKey;
import dev.echo.client.scripting.api.SettingApi;
import dev.echo.client.setting.Setting;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

/** Parses a script object while preserving Rhino property access and callback order. */
public final class ScriptDefinitionParser {
    private ScriptDefinitionParser() {
    }

    public static ScriptDefinition parse(Scriptable scope, String id) {
        NativeObject script = readScript(scope, id);
        if (script == null) {
            return null;
        }
        Category category = parseCategory(id, ScriptProperties.string(script, "category", "UTILITY"));
        if (category == null) {
            return null;
        }
        Scriptable events = ScriptProperties.object(script, "events");
        return new ScriptDefinition(
            id,
            ScriptProperties.string(script, "name", id),
            ScriptProperties.string(script, "description", ""),
            category,
            InputKey.parse(ScriptProperties.string(script, "key", "")), // UI04 text parser remains UNIMPLEMENTED.
            readSettings(ScriptProperties.object(script, "settings")),
            readEvents(events, id),
            ScriptProperties.function(events, "enable"),
            ScriptProperties.function(events, "disable"),
            scope,
            script
        );
    }

    private static NativeObject readScript(Scriptable scope, String id) {
        Object value = scope.get("script", scope);
        if (value instanceof NativeObject script) {
            return script;
        }
        ScriptErrorReporter.report(ScriptEngine.LOGGER, id, "must define a 'script' object");
        return null;
    }

    private static List<Setting> readSettings(Scriptable object) {
        List<Setting> settings = new ArrayList<>();
        if (object == null) {
            return settings;
        }
        for (Object id : object.getIds()) {
            Object value = ScriptProperties.property(object, id);
            if (value instanceof SettingApi wrapper) {
                settings.add(wrapper.getSetting());
            }
        }
        return settings;
    }

    private static Map<String, Function> readEvents(Scriptable events, String id) {
        Map<String, Function> callbacks = new LinkedHashMap<>();
        if (events == null) {
            return callbacks;
        }
        for (Object eventId : events.getIds()) {
            String name = String.valueOf(eventId);
            Object value = ScriptProperties.property(events, eventId);
            if (value instanceof Function callback) {
                // Resolve the live dispatcher names after reading this property,
                // just as the original did for each function-valued property.
                if (ScriptCallbacks.eventNames().contains(name)) {
                    callbacks.put(name, callback);
                } else if (!name.equals("enable") && !name.equals("disable")) {
                    ScriptEngine.LOGGER.warn("[{}] unknown event '{}' ignored", id, name);
                }
            }
        }
        return callbacks;
    }

    private static Category parseCategory(String id, String name) {
        return switch (name.toUpperCase()) {
            case "COMBAT" -> Category.COMBAT;
            case "MOVEMENT" -> Category.MOVEMENT;
            case "RENDER" -> Category.RENDER;
            case "WORLD" -> Category.WORLD;
            case "UTILITY" -> Category.UTILITY;
            default -> {
                ScriptErrorReporter.report(ScriptEngine.LOGGER, id, "unknown category '" + name + "'");
                yield null;
            }
        };
    }
}
