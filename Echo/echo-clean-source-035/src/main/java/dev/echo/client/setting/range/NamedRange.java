package dev.echo.client.setting.range;

/** Mutable endpoints and enablement for one member of a named range group. */
public final class NamedRange {
    private final CharSequence label;
    private boolean enabled = true;
    private float lower;
    private float upper;

    public NamedRange(CharSequence label) { this(label, 0.0f, Float.MAX_VALUE); }

    public NamedRange(CharSequence label, float lower, float upper) {
        this.label = label;
        this.lower = lower;
        this.upper = upper;
    }

    public NamedRange disabled() {
        enabled = false;
        return this;
    }

    public void constrain(float first, float second, RangeBounds bounds) {
        float a = round(Math.max(bounds.minimum(), Math.min(bounds.maximum(), first)));
        float b = round(Math.max(bounds.minimum(), Math.min(bounds.maximum(), second)));
        lower = Math.min(a, b);
        upper = Math.max(a, b);
    }

    private static float round(float value) { return Math.round(value * 10000.0f) / 10000.0f; }
    public CharSequence getLabel() { return label; }
    public boolean isEnabled() { return enabled; }
    public float getLower() { return lower; }
    public float getUpper() { return upper; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
