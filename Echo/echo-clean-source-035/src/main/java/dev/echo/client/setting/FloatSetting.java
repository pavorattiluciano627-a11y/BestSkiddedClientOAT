package dev.echo.client.setting;

import java.util.function.Predicate;

public class FloatSetting extends Setting {
    private float value;
    private float minimum;
    private float maximum;
    private float step;
    private CharSequence unit = "";
    private String cachedUnitText = "";
    public boolean allowOutOfRange;
    private float displayOverride = Float.NaN;
    private CharSequence displayOverrideText;

    public FloatSetting(CharSequence label, float value, float minimum, float maximum, float step) {
        super(label);
        this.value = value;
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
    }

    public FloatSetting(CharSequence label, float value, float minimum, float maximum, float step,
                        CharSequence unit) {
        this(label, value, minimum, maximum, step);
        this.unit = unit;
        cachedUnitText = null;
    }

    public FloatSetting(CharSequence label, float value, float minimum, float maximum, float step,
                        String unit) {
        this(label, value, minimum, maximum, step);
        this.unit = unit;
        cachedUnitText = unit;
    }

    public FloatSetting(CharSequence label, float value, float minimum, float maximum, float step,
                        Predicate<Object> visibleWhen) {
        this(label, value, minimum, maximum, step);
        setVisibilityPredicate(visibleWhen);
    }

    public FloatSetting(CharSequence label, float value, float minimum, float maximum, float step,
                        CharSequence unit, Predicate<Object> visibleWhen) {
        this(label, value, minimum, maximum, step, unit);
        setVisibilityPredicate(visibleWhen);
    }

    public FloatSetting(CharSequence label, float value, float minimum, float maximum, float step,
                        String unit, Predicate<Object> visibleWhen) {
        this(label, value, minimum, maximum, step, unit);
        setVisibilityPredicate(visibleWhen);
    }

    public FloatSetting(String label, float value, float minimum, float maximum, float step,
                        Predicate<Object> visibleWhen) {
        this((CharSequence) label, value, minimum, maximum, step, visibleWhen);
    }

    public FloatSetting(String label, float value, float minimum, float maximum, float step) {
        this((CharSequence) label, value, minimum, maximum, step);
    }

    public FloatSetting(String label, float value, float minimum, float maximum, float step, String unit) {
        this((CharSequence) label, value, minimum, maximum, step, unit);
    }

    public FloatSetting(String label, float value, float minimum, float maximum, float step,
                        String unit, Predicate<Object> visibleWhen) {
        this((CharSequence) label, value, minimum, maximum, step, unit, visibleWhen);
    }

    public String getUnitText() {
        if (cachedUnitText != null) return cachedUnitText;
        if (unit == null) return null;
        cachedUnitText = unit.toString();
        return cachedUnitText;
    }

    @Override public CharSequence getUnit() { return unit; }

    public FloatSetting displayAs(float specialValue, CharSequence text) {
        displayOverride = specialValue;
        displayOverrideText = text;
        return this;
    }

    public CharSequence getDisplayOverride() {
        if (Float.isNaN(displayOverride) || displayOverrideText == null || value != displayOverride) {
            return null;
        }
        return displayOverrideText;
    }

    public void increment() { setValue(Math.min(value + step, maximum)); }
    public void decrement() { setValue(Math.max(value - step, minimum)); }

    public void setValue(float value) {
        this.value = allowOutOfRange ? value : Math.max(minimum, Math.min(maximum, value));
        notifyChanged();
    }

    public void setValueSilently(float value) {
        this.value = Math.max(minimum, Math.min(maximum, value));
    }

    @Override public String getTypeId() { return "flt"; }
    public float getValue() { return value; }
    public float getMinimum() { return minimum; }
    public float getMaximum() { return maximum; }
    public float getStep() { return step; }
    public float getDisplayOverrideValue() { return displayOverride; }
    public CharSequence getDisplayOverrideText() { return displayOverrideText; }
    public void setMinimum(float minimum) { this.minimum = minimum; }
    public void setMaximum(float maximum) { this.maximum = maximum; }
    public void setStep(float step) { this.step = step; }
    public void setUnit(CharSequence unit) { this.unit = unit; }
    public void setCachedUnitText(String text) { cachedUnitText = text; }
    public void setDisplayOverrideValue(float value) { displayOverride = value; }
    public void setDisplayOverrideText(CharSequence text) { displayOverrideText = text; }
    public void setAllowOutOfRange(boolean allow) { allowOutOfRange = allow; }
    public boolean isOutOfRangeAllowed() { return allowOutOfRange; }
}
