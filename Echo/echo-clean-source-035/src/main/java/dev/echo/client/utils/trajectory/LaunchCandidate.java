package dev.echo.client.utils.trajectory;

/** Candidate scored launch, including adjacent launch-delay robustness. */
public record LaunchCandidate(float yaw, float pitch, double hitFraction,
        double robustnessScore, int meanHitTick) {
}
