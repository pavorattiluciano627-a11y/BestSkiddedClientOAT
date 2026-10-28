package dev.echo.client.ui.animation;

/** Frequency in hertz and dimensionless damping ratio for a physical spring. */
public record SpringSpec(float frequencyHz, float dampingRatio) {
    public static final SpringSpec CRITICAL_4_5_HZ = new SpringSpec(4.5f, 1.0f);
    public static final SpringSpec CRITICAL_10_HZ = new SpringSpec(10.0f, 1.0f);
    public static final SpringSpec UNDERDAMPED_3_8_HZ = new SpringSpec(3.8f, 0.82f);
    public static final SpringSpec UNDERDAMPED_3_3_HZ = new SpringSpec(3.3f, 0.9f);

    public SpringSpec {
        // Frequency NaN fails; damping NaN passes, matching the original branches.
        if (!(frequencyHz > 0.0f) || dampingRatio < 0.0f) {
            throw new IllegalArgumentException("Invalid physical spring");
        }
    }
}
