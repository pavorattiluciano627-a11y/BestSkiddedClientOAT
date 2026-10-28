package dev.echo.client.utils;

/**
 * A nanosecond-clock timer whose public duration queries return milliseconds.
 * Arithmetic deliberately retains Java's signed overflow and truncating casts.
 */
public class ElapsedTimer {
    private long startedAtNanos;

    /** Starts the timer through the overridable reset operation. */
    public ElapsedTimer() {
        reset();
    }

    /** Samples elapsed time before converting the seconds threshold to a long. */
    public boolean hasElapsedSeconds(double seconds) {
        return nanosToMillis(System.nanoTime() - startedAtNanos) >= (long) (seconds * 1000.0);
    }

    public boolean hasElapsedMillis(long milliseconds) {
        return nanosToMillis(System.nanoTime() - startedAtNanos) >= milliseconds;
    }

    /** Samples once for the comparison, then takes a new timestamp only on reset. */
    public boolean resetIfElapsed(long milliseconds) {
        boolean elapsed = nanosToMillis(System.nanoTime() - startedAtNanos) >= milliseconds;
        if (elapsed) {
            reset();
        }
        return elapsed;
    }

    /** Backdates the timestamp without clamping negative values or detecting overflow. */
    public void setElapsedMillis(long milliseconds) {
        startedAtNanos = System.nanoTime() - milliseconds * 1_000_000L;
    }

    public long elapsedMillis() {
        return nanosToMillis(System.nanoTime() - startedAtNanos);
    }

    public void reset() {
        startedAtNanos = System.nanoTime();
    }

    public long nanosToMillis(long nanoseconds) {
        return nanoseconds / 1_000_000L;
    }

    /** Separate original elapsed query; does not dispatch through elapsedMillis(). */
    public long getElapsedMillis() {
        return nanosToMillis(System.nanoTime() - startedAtNanos);
    }
}
