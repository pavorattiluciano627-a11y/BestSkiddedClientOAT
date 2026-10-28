package dev.echo.client.setting;

import dev.echo.client.utils.RandomMath;
import java.util.function.Predicate;

public class FloatRangeSetting extends Setting {
    private float lowerValue;
    private float upperValue;
    private float minimum;
    private float maximum;
    private float step;
    private CharSequence unit = "";
    private CharSequence displayUnit = "";
    private boolean allowOutOfRange;

    public FloatRangeSetting(CharSequence label, float lower, float upper,
                             float minimum, float maximum, float step) {
        super(label);
        lowerValue = lower;
        upperValue = upper;
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
    }

    public FloatRangeSetting(CharSequence label, float lower, float upper,
                             float minimum, float maximum, float step, CharSequence unit) {
        this(label, lower, upper, minimum, maximum, step);
        this.unit = unit;
        displayUnit = unit;
    }

    public FloatRangeSetting(CharSequence label, float lower, float upper, float minimum,
                             float maximum, float step, Predicate<Object> visibleWhen) {
        this(label, lower, upper, minimum, maximum, step);
        setVisibilityPredicate(visibleWhen);
    }

    public FloatRangeSetting(CharSequence label, float lower, float upper, float minimum,
                             float maximum, float step, CharSequence unit, Predicate<Object> visibleWhen) {
        this(label, lower, upper, minimum, maximum, step, unit);
        setVisibilityPredicate(visibleWhen);
    }

    public FloatRangeSetting(String label, float lower, float upper, float minimum, float maximum, float step) {
        this((CharSequence) label, lower, upper, minimum, maximum, step);
    }

    public FloatRangeSetting(String label, float lower, float upper, float minimum, float maximum, float step, String unit) {
        this((CharSequence) label, lower, upper, minimum, maximum, step, (CharSequence) unit);
    }

    public FloatRangeSetting(String label, float lower, float upper, float minimum, float maximum, float step,
                             String unit, Predicate<Object> visibleWhen) {
        this((CharSequence) label, lower, upper, minimum, maximum, step, (CharSequence) unit, visibleWhen);
    }

    public FloatRangeSetting(String label, float lower, float upper, float minimum, float maximum, float step,
                             Predicate<Object> visibleWhen) {
        this((CharSequence) label, lower, upper, minimum, maximum, step, visibleWhen);
    }

    // Unlike single-value sliders, endpoint mutations do not invoke the base callback.
    public void setLower(float value) {
        lowerValue = allowOutOfRange ? value : Math.max(minimum, Math.min(maximum, value));
    }

    public void setUpper(float value) {
        upperValue = allowOutOfRange ? value : Math.max(minimum, Math.min(maximum, value));
    }

    public void incrementLower() { setLower(Math.min(upperValue, Math.min(maximum, lowerValue + step))); }
    public void decrementLower() { setLower(Math.max(minimum, lowerValue - step)); }
    public void incrementUpper() { setUpper(Math.min(maximum, upperValue + step)); }
    public void decrementUpper() { setUpper(Math.max(lowerValue, Math.max(minimum, upperValue - step))); }
    public float getLowerFraction() { return (lowerValue - minimum) / (maximum - minimum); }
    public float getUpperFraction() { return (upperValue - minimum) / (maximum - minimum); }
    public float getWidth() { return upperValue - lowerValue; }

    public float sampleUniform() {
        return (float) (Math.random() * (upperValue - lowerValue)) + lowerValue;
    }

    /** Uses the client-wide generator, including its externally replaced state. */
    public float sampleGaussian() {
        float mean = (lowerValue + upperValue) / 2.0f;
        float deviation = (upperValue - lowerValue) / 6.0f;
        float sample = (float) (RandomMath.random.nextGaussian() * deviation + mean);
        return Math.max(lowerValue, Math.min(upperValue, sample));
    }

    @Override public CharSequence getUnit() { return unit; }
    @Override public String getTypeId() { return "rng"; }
    public float getLower() { return lowerValue; }
    public float getUpper() { return upperValue; }
    public float getMinimum() { return minimum; }
    public float getMaximum() { return maximum; }
    public float getStep() { return step; }
    public CharSequence getDisplayUnit() { return displayUnit; }
    public boolean isOutOfRangeAllowed() { return allowOutOfRange; }
    public void setMinimum(float minimum) { this.minimum = minimum; }
    public void setMaximum(float maximum) { this.maximum = maximum; }
    public void setStep(float step) { this.step = step; }
    public void setUnit(CharSequence unit) { this.unit = unit; }
    public void setDisplayUnit(CharSequence unit) { displayUnit = unit; }
    public void setAllowOutOfRange(boolean allow) { allowOutOfRange = allow; }
}
