package dev.echo.client.ui.animation;

import dev.echo.client.ui.UiId;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Structured candidate for the complete animation registry.
 * Pending: direct callsite proofs, UiId guard invariants, and shared accessor names.
 * This file is analysis source and is not a production implementation claim.
 */
public final class AnimationRegistry {
    private final Map<AnimationKey, AnimatedScalar> animations = new HashMap<>();
    private double timeSeconds;
    private float deltaSeconds;
    private float durationScale = 1.0F;
    private long frame;

    /** Starts a frame, limiting integration to at most fifty milliseconds. */
    public void beginFrame(double timeSeconds, float deltaSeconds) {
        this.timeSeconds = timeSeconds;
        this.deltaSeconds = Math.max(0.0F, Math.min(0.05F, deltaSeconds));
        frame++;
    }

    /** Retargets a tween continuously from its evaluated position. */
    public float tween(UiId component, String property, float initialValue, float target,
                       TransitionSpec transition) {
        AnimatedScalar animation = animations.computeIfAbsent(
                new AnimationKey(component.value(), property), key -> new AnimatedScalar(initialValue));
        animation.lastUsedFrame = frame;
        if (Float.compare(animation.target, target) != 0) {
            animation.current = evaluateTween(animation);
            animation.start = animation.current;
            animation.target = target;
            animation.startedAtSeconds = timeSeconds;
            animation.transition = transition;
        }
        animation.current = evaluateTween(animation);
        return animation.current;
    }

    /** Integrates a spring in bounded, semi-implicit Euler steps. */
    public float spring(UiId component, String property, float initialValue, float target,
                        SpringSpec spring) {
        AnimatedScalar animation = animations.computeIfAbsent(
                new AnimationKey(component.value(), property), key -> new AnimatedScalar(initialValue));
        animation.lastUsedFrame = frame;
        animation.target = target;
        if (durationScale == 0.0F) {
            animation.current = target;
            animation.velocity = 0.0F;
            return target;
        }

        float elapsed = deltaSeconds / Math.max(durationScale, 1.0E-4F);
        int steps = Math.max(1, (int) Math.ceil(elapsed / 0.004166667F));
        float stepSeconds = elapsed / steps;
        float angularFrequency = (float) (Math.PI * 2 * spring.frequencyHz());
        float stiffness = angularFrequency * angularFrequency;
        float damping = 2.0F * spring.dampingRatio() * angularFrequency;
        for (int step = 0; step < steps; step++) {
            float acceleration = stiffness * (target - animation.current) - damping * animation.velocity;
            animation.velocity += acceleration * stepSeconds;
            animation.current += animation.velocity * stepSeconds;
        }
        if (Math.abs(animation.current - target) < 1.0E-4F
                && Math.abs(animation.velocity) < 1.0E-4F) {
            animation.current = target;
            animation.velocity = 0.0F;
        }
        return animation.current;
    }

    /** Sets all positional state immediately and clears the transition and velocity. */
    public float snap(UiId component, String property, float value) {
        AnimatedScalar animation = animations.computeIfAbsent(
                new AnimationKey(component.value(), property), key -> new AnimatedScalar(value));
        animation.lastUsedFrame = frame;
        animation.current = value;
        animation.start = value;
        animation.target = value;
        animation.velocity = 0.0F;
        animation.startedAtSeconds = timeSeconds;
        animation.transition = null;
        return value;
    }

    /** Returns spring velocity, or zero when this property has never been animated. */
    public float velocity(UiId component, String property) {
        AnimatedScalar animation = animations.get(new AnimationKey(component.value(), property));
        return animation == null ? 0.0F : animation.velocity;
    }

    /** Sets the nonnegative duration multiplier; zero makes transitions immediate. */
    public void setDurationScale(float durationScale) {
        this.durationScale = Math.max(0.0F, durationScale);
    }

    /** Returns the duration multiplier. */
    public float durationScale() {
        return durationScale;
    }

    /** Removes entries strictly older than the given number of unused frames. */
    public void prune(long maximumUnusedFrames) {
        Iterator<AnimatedScalar> iterator = animations.values().iterator();
        while (iterator.hasNext()) {
            AnimatedScalar animation = iterator.next();
            if (frame - animation.lastUsedFrame > maximumUnusedFrames) {
                iterator.remove();
            }
        }
    }

    private float evaluateTween(AnimatedScalar animation) {
        if (animation.transition == null) {
            return animation.current;
        }
        float duration = animation.transition.durationSeconds() * durationScale;
        if (duration <= 0.0F) {
            return animation.target;
        }
        float progress = Math.max(0.0F, Math.min(1.0F,
                (float) ((timeSeconds - animation.startedAtSeconds) / duration)));
        if (progress >= 1.0F) {
            return animation.target;
        }
        return animation.start + (animation.target - animation.start)
                * ease(progress, animation.transition);
    }

    /** Solves the original cubic easing curve with at most five Newton iterations. */
    public static float ease(float progress, TransitionSpec transition) {
        float parameter = progress;
        for (int iteration = 0; iteration < 5; iteration++) {
            float error = CubicBezier.coordinate(parameter, transition.controlX1(), transition.controlX2()) - progress;
            float slope = CubicBezier.derivative(parameter, transition.controlX1(), transition.controlX2());
            if (Math.abs(slope) < 1.0E-5F) {
                break;
            }
            parameter = Math.max(0.0F, Math.min(1.0F, parameter - error / slope));
        }
        return CubicBezier.coordinate(parameter, transition.controlY1(), transition.controlY2());
    }
}
