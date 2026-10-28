package dev.echo.client.setting;

import java.util.function.Predicate;

public class IntSetting extends Setting {
    private int value;
    private int minimum;
    private int maximum;
    private CharSequence unit = "";
    private String cachedUnitText = "";
    private boolean allowOutOfRange;

    public IntSetting(CharSequence label, int value, int minimum, int maximum) {
        super(label);
        this.value = value;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public IntSetting(CharSequence label, int value, int minimum, int maximum, CharSequence unit) {
        this(label, value, minimum, maximum);
        this.unit = unit;
        cachedUnitText = null;
    }

    public IntSetting(CharSequence label, int value, int minimum, int maximum, String unit) {
        this(label, value, minimum, maximum);
        this.unit = unit;
        cachedUnitText = unit;
    }

    public IntSetting(CharSequence label, int value, int minimum, int maximum, Predicate<Object> visibleWhen) {
        this(label, value, minimum, maximum);
        setVisibilityPredicate(visibleWhen);
    }

    public IntSetting(CharSequence label, int value, int minimum, int maximum,
                      CharSequence unit, Predicate<Object> visibleWhen) {
        this(label, value, minimum, maximum, unit);
        setVisibilityPredicate(visibleWhen);
    }

    public IntSetting(CharSequence label, int value, int minimum, int maximum,
                      String unit, Predicate<Object> visibleWhen) {
        this(label, value, minimum, maximum, unit);
        setVisibilityPredicate(visibleWhen);
    }

    public IntSetting(String label, int value, int minimum, int maximum) {
        this((CharSequence) label, value, minimum, maximum);
    }

    public IntSetting(String label, int value, int minimum, int maximum, String unit) {
        this((CharSequence) label, value, minimum, maximum, unit);
    }

    public IntSetting(String label, int value, int minimum, int maximum, String unit, Predicate<Object> visibleWhen) {
        this((CharSequence) label, value, minimum, maximum, unit, visibleWhen);
    }

    public IntSetting(String label, int value, int minimum, int maximum, Predicate<Object> visibleWhen) {
        this((CharSequence) label, value, minimum, maximum, visibleWhen);
    }

    public void setValue(int value) {
        this.value = allowOutOfRange ? value : Math.max(minimum, Math.min(maximum, value));
        notifyChanged();
    }

    public void setValueSilently(int value) {
        this.value = Math.max(minimum, Math.min(maximum, value));
    }

    public String getUnitText() {
        if (cachedUnitText != null) return cachedUnitText;
        if (unit == null) return null;
        cachedUnitText = unit.toString();
        return cachedUnitText;
    }

    @Override public CharSequence getUnit() { return unit; }
    @Override public String getTypeId() { return "int"; }
    public int getValue() { return value; }
    public int getMinimum() { return minimum; }
    public int getMaximum() { return maximum; }
    public void setMinimum(int minimum) { this.minimum = minimum; }
    public void setMaximum(int maximum) { this.maximum = maximum; }
    public void setUnit(CharSequence unit) { this.unit = unit; }
    public void setCachedUnitText(String text) { cachedUnitText = text; }
    public void setAllowOutOfRange(boolean allow) { allowOutOfRange = allow; }
    public boolean isOutOfRangeAllowed() { return allowOutOfRange; }
}
