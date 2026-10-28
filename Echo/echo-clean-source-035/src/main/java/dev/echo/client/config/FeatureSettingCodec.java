package dev.echo.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.echo.client.input.InputKey;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.HotbarSlotsSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.setting.KeybindSetting;
import dev.echo.client.setting.MultiChoiceSetting;
import dev.echo.client.setting.NamedRangeGroupSetting;
import dev.echo.client.setting.RegistryListSetting;
import dev.echo.client.setting.Setting;
import dev.echo.client.setting.TextSetting;
import dev.echo.client.setting.range.NamedRange;
import dev.echo.client.utils.strings.Concat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

/** The twelve setting encodings in the original feature-profile format. */
final class FeatureSettingCodec {
    private FeatureSettingCodec() { }

    /** Assigns collision suffixes before hashing, in the feature's setting order. */
    static String uniqueKey(Setting setting, Set<String> usedKeys) {
        String key = Concat.shortHash(Concat.sequences(setting.getTypeId(), setting.getLabel(), setting.getUnit()));
        int suffix = 2;
        while (!usedKeys.add(key)) {
            key = Concat.shortHash(Concat.sequences(setting.getTypeId(), setting.getLabel(), setting.getUnit(),
                    Integer.toString(suffix)));
            suffix++;
        }
        return key;
    }

    /** Returns null for setting families which the original serializer omits. */
    static JsonElement encode(Setting setting) {
        if (setting instanceof BooleanSetting value) {
            return new JsonPrimitive(value.getValue());
        } else if (setting instanceof IntSetting value) {
            return new JsonPrimitive(value.getValue());
        } else if (setting instanceof FloatSetting value) {
            return new JsonPrimitive(value.getValue());
        } else if (setting instanceof FloatRangeSetting value) {
            JsonObject range = new JsonObject();
            range.addProperty("min", value.getLower());
            range.addProperty("max", value.getUpper());
            return range;
        } else if (setting instanceof ChoiceSetting value) {
            String selected = Concat.shortHash(value.getValue());
            return selected == null ? com.google.gson.JsonNull.INSTANCE : new JsonPrimitive(selected);
        } else if (setting instanceof MultiChoiceSetting value) {
            JsonObject choices = new JsonObject();
            for (Map.Entry<CharSequence, Boolean> entry : value.getOptions().entrySet()) {
                choices.addProperty(Concat.shortHash(entry.getKey()), entry.getValue());
            }
            return choices;
        } else if (setting instanceof TextSetting value) {
            String text = value.getText();
            return text == null ? com.google.gson.JsonNull.INSTANCE : new JsonPrimitive(text);
        } else if (setting instanceof ColorSetting value) {
            return new JsonPrimitive(value.getArgb());
        } else if (setting instanceof KeybindSetting value) {
            return new JsonPrimitive(value.getKey().toString());
        } else if (setting instanceof RegistryListSetting<?> value) {
            JsonArray selected = new JsonArray();
            for (Identifier id : value.getSelectedIds()) {
                selected.add(id.toString());
            }
            return selected;
        } else if (setting instanceof HotbarSlotsSetting value) {
            JsonArray slots = new JsonArray();
            for (int slot = 0; slot < 9; slot++) {
                slots.add(value.isSelected(slot));
            }
            return slots;
        } else if (setting instanceof NamedRangeGroupSetting value) {
            JsonArray ranges = new JsonArray();
            for (NamedRange range : value.getRanges()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("n", Concat.shortHash(range.getLabel()));
                entry.addProperty("e", range.isEnabled());
                if (value.hasBounds()) {
                    entry.addProperty("min", range.getLower());
                    entry.addProperty("max", range.getUpper());
                }
                ranges.add(entry);
            }
            return ranges;
        }
        return null;
    }

    /** Applies one value; the feature-level caller owns the per-setting catch boundary. */
    static void apply(Setting setting, JsonElement data) {
        Objects.requireNonNull(setting);
        if (setting instanceof BooleanSetting value) {
            value.setValue(data.getAsBoolean());
        } else if (setting instanceof IntSetting value) {
            value.setValue(data.getAsInt());
        } else if (setting instanceof FloatSetting value) {
            value.setValue(data.getAsFloat());
        } else if (setting instanceof FloatRangeSetting value) {
            JsonObject range = data.getAsJsonObject();
            if (range.has("min")) {
                value.setLower(range.get("min").getAsFloat());
            }
            if (range.has("max")) {
                value.setUpper(range.get("max").getAsFloat());
            }
        } else if (setting instanceof ChoiceSetting value) {
            CharSequence option = findChoice(value, data.getAsString());
            if (option != null) {
                value.setValue(option);
            }
        } else if (setting instanceof MultiChoiceSetting value) {
            JsonObject incoming = data.getAsJsonObject();
            Map<CharSequence, Boolean> choices = new LinkedHashMap<>(value.getOptions());
            for (Map.Entry<String, JsonElement> entry : incoming.entrySet()) {
                CharSequence option = findChoice(value, entry.getKey());
                if (option != null) {
                    choices.put(option, entry.getValue().getAsBoolean());
                }
            }
            value.replaceOptions(choices);
        } else if (setting instanceof TextSetting value) {
            value.setText(data.getAsString());
        } else if (setting instanceof ColorSetting value) {
            value.setArgb(data.getAsInt());
        } else if (setting instanceof KeybindSetting value) {
            value.setKey(readKey(data));
        } else if (setting instanceof RegistryListSetting<?> value) {
            if (data.isJsonArray()) {
                Set<Identifier> ids = new HashSet<>();
                for (JsonElement entry : data.getAsJsonArray()) {
                    try {
                        ids.add(Identifier.of(entry.getAsString()));
                    } catch (Exception ignored) {
                        // One malformed identifier does not discard the other selections.
                    }
                }
                value.replaceSelectedIds(ids);
            }
        } else if (setting instanceof HotbarSlotsSetting value) {
            if (data.isJsonArray()) {
                JsonArray slots = data.getAsJsonArray();
                for (int slot = 0; slot < 9; slot++) {
                    value.setSelected(slot, slot < slots.size() && slots.get(slot).getAsBoolean());
                }
            }
        } else if (setting instanceof NamedRangeGroupSetting value && data.isJsonArray()) {
            applyRanges(value, data.getAsJsonArray());
        }
    }

    static CharSequence findChoice(ChoiceSetting setting, String hash) {
        if (hash == null) {
            return null;
        }
        for (CharSequence option : setting.getOptions()) {
            if (hash.equals(Concat.shortHash(option))) {
                return option;
            }
        }
        return null;
    }

    static CharSequence findChoice(MultiChoiceSetting setting, String hash) {
        if (hash == null) {
            return null;
        }
        for (CharSequence option : setting.getOptions().keySet()) {
            if (hash.equals(Concat.shortHash(option))) {
                return option;
            }
        }
        return null;
    }

    /** Moves matched live ranges to successive positions; omitted ranges keep their relative order. */
    static void applyRanges(NamedRangeGroupSetting setting, JsonArray data) {
        int destination = 0;
        for (JsonElement element : data) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject incoming = element.getAsJsonObject();
            String hash = ProfileResponseCodec.textField(incoming, "n");
            List<NamedRange> ranges = setting.getRanges();
            for (int index = 0; index < ranges.size(); index++) {
                NamedRange range = ranges.get(index);
                if (!Concat.shortHash(range.getLabel()).equals(hash)) {
                    continue;
                }
                if (incoming.has("e")) {
                    range.setEnabled(incoming.get("e").getAsBoolean());
                }
                if (incoming.has("min") && incoming.has("max")) {
                    setting.setRange(range, incoming.get("min").getAsFloat(), incoming.get("max").getAsFloat());
                }
                setting.moveRange(index, destination++);
                break;
            }
        }
    }

    /** Reads labels, Minecraft translation keys, and the legacy sign-bit mouse encoding. */
    static InputKey readKey(JsonElement data) {
        if (data == null || !data.isJsonPrimitive()) {
            return InputKey.NONE;
        }
        if (!data.getAsJsonPrimitive().isNumber()) {
            String text = data.getAsString();
            if (text.startsWith("key.") || text.startsWith("scancode.")) {
                InputUtil.Key translated;
                try {
                    translated = InputUtil.fromTranslationKey(text);
                } catch (IllegalArgumentException ignored) {
                    return InputKey.NONE;
                }
                return translated.getCategory() == InputUtil.Type.MOUSE
                        ? InputKey.fromMouseCode(translated.getCode()) : InputKey.fromCode(translated.getCode());
            }
            return InputKey.parse(text);
        }
        int code = data.getAsInt();
        if (code == -1) {
            return InputKey.NONE;
        }
        return (code & Integer.MIN_VALUE) != 0 ? InputKey.fromMouseCode(code & 255) : InputKey.fromCode(code);
    }
}
