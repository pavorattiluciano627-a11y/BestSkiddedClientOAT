package dev.echo.client.setting;

import java.util.function.Predicate;

public class BooleanSetting extends Setting {
    private boolean value;

    public BooleanSetting(CharSequence label, boolean value) {
        super(label);
        this.value = value;
    }

    public BooleanSetting(CharSequence label, boolean value, Predicate<Object> visibleWhen) {
        this(label, value);
        setVisibilityPredicate(visibleWhen);
    }

    public BooleanSetting(String label, boolean value) {
        this((CharSequence) label, value);
    }

    public BooleanSetting(String label, boolean value, Predicate<Object> visibleWhen) {
        this((CharSequence) label, value, visibleWhen);
    }

    /** Effective value includes availability, but deliberately excludes the visibility predicate. */
    public boolean getValue() {
        return value && isAvailable();
    }

    public void setValue(boolean value) {
        this.value = value;
        notifyChanged();
    }

    @Override public String getTypeId() { return "boo"; }
}
