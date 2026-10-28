package dev.echo.client.events;

/** Lifecycle phases, in the original constructor-name and ordinal order. */
public enum EventPhase {
    NONE,
    EARLY,
    PRE,
    POST;

    private static boolean dispatchState;

    static {
        if (!getDispatchState()) {
            setDispatchState(true);
        }
    }

    /** Original shared control write; this state is not a per-cycle input latch. */
    public static void setDispatchState(boolean value) {
        dispatchState = value;
    }

    public static boolean getDispatchState() {
        return dispatchState;
    }

    public static boolean isDispatchStateClear() {
        return !getDispatchState();
    }
}
