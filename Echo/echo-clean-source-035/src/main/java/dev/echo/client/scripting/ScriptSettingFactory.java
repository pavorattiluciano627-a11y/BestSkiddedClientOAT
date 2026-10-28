package dev.echo.client.scripting;

import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.HotbarSlotsSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.setting.MultiChoiceSetting;
import dev.echo.client.setting.TextSetting;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;

/** Concrete setting construction; the ScriptSetting wrapper is installed by its API owner. */
public final class ScriptSettingFactory {
    private ScriptSettingFactory() {
    }

    public static BooleanSetting booleanSetting(Object[] arguments) {
        String name = SettingArguments.string(arguments, 0, "setting");
        boolean value = arguments.length > 1 && ScriptValues.toBoolean(arguments[1]);
        return new BooleanSetting(name, value);
    }

    public static IntSetting integerSetting(Object[] arguments) {
        String name = SettingArguments.string(arguments, 0, "setting");
        int value = SettingArguments.integer(arguments, 1, 0);
        int minimum = SettingArguments.integer(arguments, 2, 0);
        int maximum = SettingArguments.integer(arguments, 3, 100);
        return new IntSetting(name, value, minimum, maximum);
    }

    public static FloatSetting floatSetting(Object[] arguments) {
        String name = SettingArguments.string(arguments, 0, "setting");
        float value = SettingArguments.floating(arguments, 1, 0.0F);
        float minimum = SettingArguments.floating(arguments, 2, 0.0F);
        float maximum = SettingArguments.floating(arguments, 3, 1.0F);
        float step = SettingArguments.floating(arguments, 4, 0.1F);
        return new FloatSetting(name, value, minimum, maximum, step);
    }

    public static FloatRangeSetting rangeSetting(Object[] arguments) {
        String name = SettingArguments.string(arguments, 0, "setting");
        float lower = SettingArguments.floating(arguments, 1, 0.0F);
        float upper = SettingArguments.floating(arguments, 2, 100.0F);
        float minimum = SettingArguments.floating(arguments, 3, 0.0F);
        float maximum = SettingArguments.floating(arguments, 4, 100.0F);
        float step = SettingArguments.floating(arguments, 5, 1.0F);
        return new FloatRangeSetting(name, lower, upper, minimum, maximum, step);
    }

    public static MultiChoiceSetting booleanGroupSetting(Object[] arguments) {
        String name = SettingArguments.string(arguments, 0, "setting");
        NativeArray options = arguments.length > 1 && arguments[1] instanceof NativeArray array
            ? array : new NativeArray(0L);
        CharSequence[] labels = new CharSequence[(int) options.getLength()];
        for (int index = 0; index < labels.length; index++) {
            labels[index] = Context.toString(options.get(index, options));
        }
        return new MultiChoiceSetting(name, labels);
    }

    public static ChoiceSetting choiceSetting(Object[] arguments) {
        String name = SettingArguments.string(arguments, 0, "setting");
        String value = SettingArguments.string(arguments, 1, "");
        NativeArray options = arguments.length > 2 && arguments[2] instanceof NativeArray array
            ? array : new NativeArray(0L);
        CharSequence[] labels = new CharSequence[(int) options.getLength()];
        for (int index = 0; index < labels.length; index++) {
            labels[index] = Context.toString(options.get(index, options));
        }
        return new ChoiceSetting(name, value, labels);
    }

    public static TextSetting textSetting(Object[] arguments) {
        String name = SettingArguments.string(arguments, 0, "setting");
        String placeholder = SettingArguments.string(arguments, 1, "");
        return new TextSetting(name, placeholder);
    }

    public static ColorSetting colorSetting(Object[] arguments) {
        String name = SettingArguments.string(arguments, 0, "setting");
        int red = SettingArguments.integer(arguments, 1, 255);
        int green = SettingArguments.integer(arguments, 2, 255);
        int blue = SettingArguments.integer(arguments, 3, 255);
        int alpha = SettingArguments.integer(arguments, 4, 255);
        return new ColorSetting(name, red, green, blue, alpha);
    }

    public static HotbarSlotsSetting hotbarSlotsSetting(Object[] arguments) {
        return new HotbarSlotsSetting(SettingArguments.string(arguments, 0, "setting"));
    }
}
