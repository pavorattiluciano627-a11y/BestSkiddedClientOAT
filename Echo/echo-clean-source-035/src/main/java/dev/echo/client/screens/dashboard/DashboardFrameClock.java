package dev.echo.client.screens.dashboard;

/** Frame delta state used by the dashboard's original animation update. */
public final class DashboardFrameClock {
    private long previousFrameNanos;
    private float deltaSeconds;

    /** Starts the clock at an already sampled frame timestamp. */
    public DashboardFrameClock(long initialFrameNanos) {
        previousFrameNanos = initialFrameNanos;
    }

    /** Samples the monotonic clock and limits animation steps to fifty milliseconds. */
    public void update() {
        long now = System.nanoTime();
        deltaSeconds = Math.min(0.05f,
                Math.max(0.0f, (float) (now - previousFrameNanos) / 1.0e9f));
        previousFrameNanos = now;
    }

    /** Returns the most recently calculated step. */
    public float deltaSeconds() {
        return deltaSeconds;
    }
}
