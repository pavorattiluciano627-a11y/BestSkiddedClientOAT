package dev.echo.client.ui.animation;

/**
 * Original millisecond-clock scalar tween.
 * Source-complete candidate: original sentinel and direct-target/cache domains remain explicit.
 */
public class Tween {
    private Easing easing;
    private double durationMillis;
    private long startTimeMillis;
    private double startValue;
    private double targetValue;

    /** Creates a zero-to-zero tween using the original wall clock. */
    public Tween(Easing easing, double durationMillis) {
        this.easing = easing;
        startTimeMillis = System.currentTimeMillis();
        this.durationMillis = durationMillis;
    }

    /** Reports whether the current wall-clock interval has ended. */
    public boolean isFinished() {
        return System.currentTimeMillis() - startTimeMillis >= durationMillis;
    }

    /**
     * Retargets an unfinished tween without restarting its clock. Once finished,
     * starts a new interval from its previous target.
     */
    public void setTarget(double target) {
        if (!isFinished()) {
            targetValue = target;
        } else {
            start(targetValue, target);
        }
    }

    /** Replaces both endpoints and restarts the original wall-clock interval. */
    public void start(double from, double to) {
        startValue = from;
        targetValue = to;
        startTimeMillis = System.currentTimeMillis();
    }

    /** Samples the current eased value, preserving the original float elapsed-time conversion. */
    public double value() {
        if (isFinished()) {
            return targetValue;
        }
        long now = System.currentTimeMillis();
        float elapsedMillis = now - startTimeMillis;
        double progress = elapsedMillis / durationMillis;
        double eased = Math.clamp(easing.function().apply(progress), 0.0, 1.0);
        return startValue + (targetValue - startValue) * eased;
    }

    /** Replaces the easing function without restarting the interval. */
    public void setEasing(Easing easing) { this.easing = easing; }

    /** Replaces the duration without restarting the interval. */
    public void setDurationMillis(double durationMillis) { this.durationMillis = durationMillis; }

    /** Returns the stored start endpoint. */
    public double startValue() { return startValue; }

    /** Returns the stored target endpoint. */
    public double targetValue() { return targetValue; }
}
