package dev.echo.client.hud;

/** Original notification stacking, staggering, and fade calculations. */
public final class NotificationTiming {
    private NotificationTiming() {
    }

    /** Returns the original transition duration in seconds. */
    public static float transitionSeconds() {
        return 0.22f;
    }

    /** Calculates a remaining duration with the original 250 ms floor. */
    public static long remainingMillis(long duration, long elapsed) {
        return Math.max(250L, Math.max(0L, duration) - Math.max(0L, elapsed));
    }

    /** Fades older entries geometrically while keeping them at least 28% opaque. */
    public static float stackOpacity(int index, int count) {
        int size = Math.max(1, count);
        int clampedIndex = Math.max(0, Math.min(index, size - 1));
        int age = size - 1 - clampedIndex;
        return Math.max(0.28f, (float) Math.pow(0.82f, age));
    }

    /** Calculates the original pairwise notification appearance delay. */
    public static long staggerMillis(int index, long duration) {
        int clampedIndex = Math.max(0, index);
        long clampedDuration = Math.max(1L, duration);
        long pair = clampedIndex / 2;
        long secondInPair = clampedIndex % 2;
        return 40L + pair * (clampedDuration + 300L) + secondInPair * 250L;
    }

    /** Holds a value for 250 ms, then linearly fades it over the remaining duration. */
    public static float fadeAfterHold(float value, long now, long startedAt, long duration) {
        float fade = Math.max(0.0f, 1.0f
                - (float) Math.max(0L, now - startedAt - 250L)
                / Math.max(250.0f, (float) duration - 250.0f));
        return Math.max(0.0f, value) * fade;
    }
}
