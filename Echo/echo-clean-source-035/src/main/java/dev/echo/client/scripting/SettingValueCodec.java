package dev.echo.client.scripting;

import dev.echo.client.input.InputKey;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.setting.KeybindSetting;
import dev.echo.client.setting.Setting;
import dev.echo.client.setting.TextSetting;
import java.util.Objects;
import org.mozilla.javascript.NativeObject;

/** Bidirectional setting conversion; unsupported writes retain the original no-op behavior. */
public class SettingValueCodec {
    public static Object toJs(Setting setting) {
        return SettingValueReader.toJs(setting);
    }

    public static void fromJs(Setting setting, Object value) {
        Objects.requireNonNull(setting);
        switch (setting) {
            case BooleanSetting target -> target.setValue(ScriptValues.toBoolean(value));
            case IntSetting target -> {
                if (value instanceof Number number) {
                    target.setValue(number.intValue());
                }
            }
            case FloatSetting target -> {
                if (value instanceof Number number) {
                    target.setValue(number.floatValue());
                }
            }
            case ChoiceSetting target -> {
                if (value instanceof String text) {
                    target.setValue(text);
                }
            }
            case TextSetting target -> {
                if (value instanceof String text) {
                    target.setText(text);
                }
            }
            case ColorSetting target -> {
                if (value instanceof Number number) {
                    // The script writes RGB; the existing alpha channel survives.
                    target.setRgb(number.intValue());
                }
            }
            case KeybindSetting target -> {
                if (value instanceof CharSequence text) {
                    target.setKey(InputKey.parse(text.toString())); // UI04 text parser remains UNIMPLEMENTED.
                }
            }
            case FloatRangeSetting target -> {
                if (value instanceof NativeObject range) {
                    Object minimum = range.get("min", range);
                    Object maximum = range.get("max", range);
                    if (minimum instanceof Number number) {
                        target.setLower(number.floatValue());
                    }
                    if (maximum instanceof Number number) {
                        target.setUpper(number.floatValue());
                    }
                }
            }
            default -> {
                // The original switch has no mutation for other setting kinds.
            }
        }
    }
}
