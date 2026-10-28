package dev.echo.client.scripting;

import dev.echo.client.features.Category;
import dev.echo.client.input.InputKey;
import dev.echo.client.setting.Setting;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/** Script metadata and callbacks. Collections and scopes retain their original identities. */
public record ScriptDefinition(
    String id,
    String name,
    String description,
    Category category,
    InputKey key,
    List<Setting> settings,
    Map<String, Function> events,
    Function onEnable,
    Function onDisable,
    Scriptable scope,
    Scriptable scriptObject
) {
}
