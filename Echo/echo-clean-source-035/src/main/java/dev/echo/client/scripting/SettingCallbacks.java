package dev.echo.client.scripting;

import dev.echo.client.setting.Setting;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/** Direct Rhino callback bodies; the wrapper reads its current setting at invocation time. */
public final class SettingCallbacks {
    private SettingCallbacks() {
    }

    public static void callOnChange(Function callback, Context context, Scriptable self, Setting currentSetting) {
        callback.call(context, self, self, new Object[] {SettingValueReader.toJs(currentSetting)});
    }

    public static boolean testVisibility(Function callback, Context context, Scriptable self) {
        return ScriptValues.toBoolean(callback.call(context, self, self, new Object[0]));
    }
}
