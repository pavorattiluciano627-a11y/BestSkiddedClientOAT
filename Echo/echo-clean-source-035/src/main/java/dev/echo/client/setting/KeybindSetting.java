package dev.echo.client.setting;

import java.util.function.Predicate;
import dev.echo.client.input.InputKey;

/** Keyboard/mouse binding and the UI's capture-listening state. */
public class KeybindSetting extends Setting {
    private InputKey key;
    private boolean listeningForKey;

    public KeybindSetting(CharSequence label, InputKey key) {
        super(label);
        this.key = key;
    }

    public KeybindSetting(CharSequence label, InputKey key, Predicate<Object> visibleWhen) {
        this(label, key);
        setVisibilityPredicate(visibleWhen);
    }

    public KeybindSetting(String label, InputKey key) { this((CharSequence) label, key); }

    public KeybindSetting(String label, InputKey key, Predicate<Object> visibleWhen) {
        this((CharSequence) label, key, visibleWhen);
    }

    public InputKey getKey() { return key; }
    public boolean isListeningForKey() { return listeningForKey; }
    public void setKey(InputKey key) { this.key = key; }
    public void setListeningForKey(boolean listening) { listeningForKey = listening; }
    @Override public String getTypeId() { return "key"; }
}
