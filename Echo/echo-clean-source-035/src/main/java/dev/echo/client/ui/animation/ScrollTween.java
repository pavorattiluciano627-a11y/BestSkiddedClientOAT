package dev.echo.client.ui.animation;

/**
 * Two-axis interpolation driven by the original scalar tween's zero-to-one interval.
 * Source-complete candidate: inherited sentinel and direct-target/cache applicability remain explicit.
 */
public class ScrollTween extends Tween {
    private float startX;
    private float startY;
    private float targetX;
    private float targetY;

    /** Creates the original two-axis scroll tween. */
    public ScrollTween(Easing easing, float durationMillis) {
        super(easing, durationMillis);
    }

    /** Replaces both two-dimensional endpoints and starts a zero-to-one interval. */
    public void start(float fromX, float fromY, float toX, float toY) {
        startX = fromX;
        startY = fromY;
        targetX = toX;
        targetY = toY;
        super.start(0.0, 1.0);
    }

    /** The original subclass deliberately ignores the inherited scalar retarget operation. */
    @Override
    public void setTarget(double target) {
        // Exact original WindowResolver.getPacket(JD)V body: return.
    }

    /** Retargets only x, restarting from the stored targets only after completion. */
    public void scrollToX(float x) {
        if (isFinished()) {
            start(targetX, targetY, x, targetY);
        } else {
            targetX = x;
        }
    }

    /** Retargets both axes, keeping the current clock while interpolation is active. */
    public void scrollTo(float x, float y) {
        if (isFinished()) {
            start(targetX, targetY, x, y);
        } else {
            targetX = x;
            targetY = y;
        }
    }

    /** Samples the original x interpolation. */
    public float x() {
        return (float) (startX + (targetX - startX) * super.value());
    }

    /** Samples the original y interpolation independently. */
    public float y() {
        return (float) (startY + (targetY - startY) * super.value());
    }

    /** Replaces the x target without testing completion or restarting the clock. */
    public void setTargetX(float x) { targetX = x; }

    /** Replaces the y target without testing completion or restarting the clock. */
    public void setTargetY(float y) { targetY = y; }
}
