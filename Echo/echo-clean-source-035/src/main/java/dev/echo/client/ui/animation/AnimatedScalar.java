package dev.echo.client.ui.animation;

/** Mutable state for one tween or spring; candidate pending constructor linkage proof. */
final class AnimatedScalar {
    float current;
    float start;
    float target;
    float velocity;
    double startedAtSeconds;
    TransitionSpec transition;
    long lastUsedFrame;

    AnimatedScalar(float initialValue) {
        target = initialValue;
        start = initialValue;
        current = initialValue;
    }
}
