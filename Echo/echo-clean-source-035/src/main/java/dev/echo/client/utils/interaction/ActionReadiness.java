package dev.echo.client.utils.interaction;

/**
 * Original readiness state machine. Public access is an explicit clean-package
 * integration adaptation; the original class and these entry points were package-private.
 */
public final class ActionReadiness {
    private static final double VERTICAL_DISPLACEMENT_THRESHOLD = 0.05;
    private ActionReadinessState state;

    public ActionReadiness() {
        state = ActionReadinessState.IDLE;
    }

    public void beginWaiting() {
        state = ActionReadinessState.WAITING;
    }

    public void update(double verticalDisplacement, boolean supported) {
        if (state == ActionReadinessState.IDLE || state == ActionReadinessState.READY) {
            return;
        }
        if (supported || state == ActionReadinessState.DESCENDING
                && verticalDisplacement > VERTICAL_DISPLACEMENT_THRESHOLD) {
            state = ActionReadinessState.READY;
        } else if (verticalDisplacement < -VERTICAL_DISPLACEMENT_THRESHOLD) {
            state = ActionReadinessState.DESCENDING;
        }
    }

    public boolean isReady() {
        return state == ActionReadinessState.READY;
    }

    public void reset() {
        state = ActionReadinessState.IDLE;
    }
}
