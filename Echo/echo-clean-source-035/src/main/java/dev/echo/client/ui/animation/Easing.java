package dev.echo.client.ui.animation;

import java.util.function.Function;

/** Original curve catalog; input/output clamping belongs to its callers. */
public enum Easing {
    LINEAR(Easing::linear),
    EASE_IN_SINE(Easing::inSine),
    EASE_OUT_SINE(Easing::outSine),
    EASE_IN_OUT_SINE(Easing::inOutSine),
    EASE_IN_QUAD(Easing::inQuad),
    EASE_OUT_QUAD(Easing::outQuad),
    EASE_IN_OUT_QUAD(Easing::inOutQuad),
    EASE_IN_CUBIC(Easing::inCubic),
    EASE_OUT_CUBIC(Easing::outCubic),
    EASE_IN_OUT_CUBIC(Easing::inOutCubic),
    EASE_IN_QUART(Easing::inQuart),
    EASE_OUT_QUART(Easing::outQuart),
    EASE_IN_OUT_QUART(Easing::inOutQuart),
    EASE_IN_QUINT(Easing::inQuint),
    EASE_OUT_QUINT(Easing::outQuint),
    EASE_IN_OUT_QUINT(Easing::inOutQuint),
    EASE_IN_EXPO(Easing::inExpo),
    EASE_OUT_EXPO(Easing::outExpo),
    EASE_IN_OUT_EXPO(Easing::inOutExpo),
    EASE_IN_CIRC(Easing::inCirc),
    EASE_OUT_CIRC(Easing::outCirc),
    EASE_IN_OUT_CIRC(Easing::inOutCirc),
    EASE_IN_BACK(Easing::inBack),
    EASE_OUT_BACK(Easing::outBack),
    EASE_IN_OUT_BACK(Easing::inOutBack),
    EASE_IN_ELASTIC(Easing::inElastic),
    EASE_OUT_ELASTIC(Easing::outElastic),
    EASE_IN_OUT_ELASTIC(Easing::inOutElastic),
    EASE_IN_BOUNCE(Easing::inBounce),
    EASE_OUT_BOUNCE(Easing::outBounce),
    EASE_IN_OUT_BOUNCE(Easing::inOutBounce);

    private final Function<Double, Double> function;

    Easing(Function<Double, Double> function) {
        this.function = function;
    }

    public Function<Double, Double> function() {
        return function;
    }

    public static Double linear(Double progress) { return progress; }
    public static Double inSine(Double progress) { return 1.0 - Math.cos(progress * Math.PI / 2.0); }
    public static Double outSine(Double progress) { return Math.sin(progress * Math.PI / 2.0); }
    public static Double inOutSine(Double progress) { return -(Math.cos(Math.PI * progress) - 1.0) / 2.0; }
    public static Double inQuad(Double progress) { return progress * progress; }
    public static Double outQuad(Double progress) { return 1.0 - (1.0 - progress) * (1.0 - progress); }

    public static Double inOutQuad(Double progress) {
        return progress < 0.5 ? 2.0 * progress * progress : 1.0 - Math.pow(-2.0 * progress + 2.0, 2.0) / 2.0;
    }

    public static Double inCubic(Double progress) { return progress * progress * progress; }
    public static Double outCubic(Double progress) { return 1.0 - Math.pow(1.0 - progress, 3.0); }

    public static Double inOutCubic(Double progress) {
        return progress < 0.5 ? 4.0 * progress * progress * progress : 1.0 - Math.pow(-2.0 * progress + 2.0, 3.0) / 2.0;
    }

    public static Double inQuart(Double progress) { return progress * progress * progress * progress; }
    public static Double outQuart(Double progress) { return 1.0 - Math.pow(1.0 - progress, 4.0); }

    public static Double inOutQuart(Double progress) {
        return progress < 0.5 ? 8.0 * progress * progress * progress * progress : 1.0 - Math.pow(-2.0 * progress + 2.0, 4.0) / 2.0;
    }

    public static Double inQuint(Double progress) { return progress * progress * progress * progress * progress; }
    public static Double outQuint(Double progress) { return 1.0 - Math.pow(1.0 - progress, 5.0); }

    public static Double inOutQuint(Double progress) {
        return progress < 0.5 ? 16.0 * progress * progress * progress * progress * progress
                : 1.0 - Math.pow(-2.0 * progress + 2.0, 5.0) / 2.0;
    }

    public static Double inExpo(Double progress) {
        return progress == 0.0 ? 0.0 : Math.pow(2.0, 10.0 * progress - 10.0);
    }

    public static Double outExpo(Double progress) {
        return progress == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * progress);
    }

    public static Double inOutExpo(Double progress) {
        if (progress == 0.0) return 0.0;
        if (progress == 1.0) return 1.0;
        return progress < 0.5 ? Math.pow(2.0, 20.0 * progress - 10.0) / 2.0
                : (2.0 - Math.pow(2.0, -20.0 * progress + 10.0)) / 2.0;
    }

    public static Double inCirc(Double progress) { return 1.0 - Math.sqrt(1.0 - Math.pow(progress, 2.0)); }
    public static Double outCirc(Double progress) { return Math.sqrt(1.0 - Math.pow(progress - 1.0, 2.0)); }

    public static Double inOutCirc(Double progress) {
        return progress < 0.5 ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * progress, 2.0))) / 2.0
                : (Math.sqrt(1.0 - Math.pow(-2.0 * progress + 2.0, 2.0)) + 1.0) / 2.0;
    }

    public static Double inBack(Double progress) {
        double overshoot = 1.70158;
        double cubicCoefficient = overshoot + 1.0;
        return cubicCoefficient * progress * progress * progress - overshoot * progress * progress;
    }

    public static Double outBack(Double progress) {
        double overshoot = 1.70158;
        double cubicCoefficient = overshoot + 1.0;
        return 1.0 + cubicCoefficient * Math.pow(progress - 1.0, 3.0) + overshoot * Math.pow(progress - 1.0, 2.0);
    }

    public static Double inOutBack(Double progress) {
        double overshoot = 1.70158;
        double adjustedOvershoot = overshoot * 1.525;
        return progress < 0.5
                ? Math.pow(2.0 * progress, 2.0) * ((adjustedOvershoot + 1.0) * 2.0 * progress - adjustedOvershoot) / 2.0
                : (Math.pow(2.0 * progress - 2.0, 2.0) * ((adjustedOvershoot + 1.0) * (progress * 2.0 - 2.0) + adjustedOvershoot) + 2.0) / 2.0;
    }

    public static Double inElastic(Double progress) {
        double period = 2.0943951023931953;
        if (progress == 0.0) return 0.0;
        if (progress == 1.0) return 1.0;
        return -Math.pow(2.0, 10.0 * progress - 10.0) * Math.sin((progress * 10.0 - 10.75) * period);
    }

    public static Double outElastic(Double progress) {
        double period = 2.0943951023931953;
        if (progress == 0.0) return 0.0;
        if (progress == 1.0) return 1.0;
        return Math.pow(2.0, -10.0 * progress) * Math.sin((progress * 10.0 - 0.75) * period) + 1.0;
    }

    public static Double inOutElastic(Double progress) {
        double period = 1.3962634015954636;
        double wave = Math.sin((20.0 * progress - 11.125) * period);
        if (progress == 0.0) return 0.0;
        if (progress == 1.0) return 1.0;
        return progress < 0.5 ? -(Math.pow(2.0, 20.0 * progress - 10.0) * wave) / 2.0
                : Math.pow(2.0, -20.0 * progress + 10.0) * wave / 2.0 + 1.0;
    }

    /** The original IN variant uses this same piecewise polynomial; do not reverse it by convention. */
    public static Double inBounce(Double progress) {
        double coefficient = 7.5625;
        double divisor = 2.75;
        if (progress < 1.0 / divisor) return coefficient * progress * progress;
        if (progress < 2.0 / divisor) {
            progress = progress - 1.5 / divisor;
            return coefficient * progress * progress + 0.75;
        }
        if (progress < 2.5 / divisor) {
            progress = progress - 2.25 / divisor;
            return coefficient * progress * progress + 0.9375;
        }
        progress = progress - 2.625 / divisor;
        return coefficient * progress * progress + 0.984375;
    }

    public static Double outBounce(Double progress) {
        double coefficient = 7.5625;
        double divisor = 2.75;
        if (progress < 1.0 / divisor) return coefficient * progress * progress;
        if (progress < 2.0 / divisor) {
            progress = progress - 1.5 / divisor;
            return coefficient * progress * progress + 0.75;
        }
        if (progress < 2.5 / divisor) {
            progress = progress - 2.25 / divisor;
            return coefficient * progress * progress + 0.9375;
        }
        progress = progress - 2.625 / divisor;
        return coefficient * progress * progress + 0.984375;
    }

    public static Double inOutBounce(Double progress) {
        return progress < 0.5 ? (1.0 - EASE_OUT_BOUNCE.function().apply(1.0 - 2.0 * progress)) / 2.0
                : (1.0 + EASE_OUT_BOUNCE.function().apply(2.0 * progress - 1.0)) / 2.0;
    }
}
