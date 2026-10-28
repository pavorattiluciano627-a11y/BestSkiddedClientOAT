package dev.echo.client.utils.trajectory;

/** Candidate launch result from sixteen deterministic spread samples. */
public record MovingTargetLaunch(float yaw, float pitch, double hitFraction, int meanHitTick) {
}
