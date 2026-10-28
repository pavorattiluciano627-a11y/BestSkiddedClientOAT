package dev.echo.client.handlers;

/** Visibility, transition style and nonnegative per-axis rotation limits. */
public record RotationSettings(
        RotationVisibility visibility,
        AimTransition transition,
        float maxYawStep,
        float maxPitchStep) {

    public RotationSettings {
        maxYawStep = Math.max(0.0F, maxYawStep);
        maxPitchStep = Math.max(0.0F, maxPitchStep);
    }

    /** Uses the same angular limit on both axes. */
    public static RotationSettings uniform(
            RotationVisibility visibility, AimTransition transition, float maxStep) {
        return new RotationSettings(visibility, transition, maxStep, maxStep);
    }
}
