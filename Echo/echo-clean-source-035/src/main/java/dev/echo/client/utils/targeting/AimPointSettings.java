package dev.echo.client.utils.targeting;

/** Immutable mode, interpolation weight and visibility/randomization preferences. */
public record AimPointSettings(AimPointMode mode, float pointWeight,
                               boolean randomize, boolean allowOccluded) {
    public AimPointSettings {
        pointWeight = Math.clamp(pointWeight, 0.0F, 100.0F);
    }

    /** Creates a fresh default configuration, as in the original factory. */
    public static AimPointSettings defaults() {
        return new AimPointSettings(AimPointMode.STRAIGHT, 50.0F, false, false);
    }
}
