package dev.echo.client.scripting;

import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.HotbarSlotsSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.setting.KeybindSetting;
import dev.echo.client.setting.MultiChoiceSetting;
import dev.echo.client.setting.Setting;
import dev.echo.client.setting.TextSetting;
import java.util.Objects;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Undefined;

/** Encodes a setting value without changing its value, callbacks or visibility. */
public final class SettingValueReader {
    private SettingValueReader() {
    }

    public static Object toJs(Setting setting) {
        Objects.requireNonNull(setting);
        return switch (setting) {
            case BooleanSetting value -> value.getValue();
            case IntSetting value -> value.getValue();
            case FloatSetting value -> value.getValue();
            case ChoiceSetting value -> value.getValue().toString();
            case TextSetting value -> value.getText();
            case ColorSetting value -> value.getArgb();
            case KeybindSetting value -> value.getKey();
            case FloatRangeSetting value -> {
                NativeObject range = new NativeObject();
                range.defineProperty("min", value.getLower(), 0);
                range.defineProperty("max", value.getUpper(), 0);
                yield range;
            }
            case MultiChoiceSetting value -> {
                NativeObject group = new NativeObject();
                value.toStringMap().forEach((name, enabled) -> group.defineProperty(name, enabled, 0));
                yield group;
            }
            case HotbarSlotsSetting value -> {
                NativeArray slots = new NativeArray(9L);
                for (int index = 0; index < 9; index++) {
                    slots.put(index, slots, value.isSelected(index));
                }
                yield slots;
            }
            default -> Undefined.instance;
        };
    }
}
