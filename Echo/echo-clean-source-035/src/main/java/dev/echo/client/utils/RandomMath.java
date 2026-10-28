package dev.echo.client.utils;

import java.util.Random;

/** Numeric and random operations share the original generator initialization boundary. */
public final class RandomMath {
    public static Random random = new Random(System.currentTimeMillis());

    public static double roundToMultiple(double value, double multiple) {
        return multiple * Math.round(value / multiple);
    }

    public static double smoothStep(double fraction, double start, double end) {
        fraction = Math.max(0.0, Math.min(1.0, fraction));
        double eased = fraction * fraction * (3.0 - 2.0 * fraction);
        return start + (end - start) * eased;
    }

    public static double lerp(double start, double end, double fraction) {
        return start + (end - start) * fraction;
    }

    public static float lerp(float start, float end, float fraction) {
        return start + (end - start) * fraction;
    }

    public static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static int randomInt(int origin, int bound) {
        return random.nextInt(origin, bound);
    }

    public static double randomDouble(double origin, double bound) {
        return origin + (bound - origin) * random.nextDouble();
    }

    /** The arithmetic is float precision even though the result is a double. */
    public static double randomFloat(float origin, float bound) {
        return origin + (bound - origin) * random.nextFloat();
    }

    public static double approach(float fraction, double current, double target) {
        int step = (int) Math.ceil(Math.abs(target - current) * fraction);
        return current < target ? Math.min(current + step, target) : Math.max(current - step, target);
    }

    public static int randomGaussianInt(int mean, int deviation) {
        return (int) (random.nextGaussian() * deviation + mean);
    }

    public static float randomRelative(float value, float fraction) {
        float radius = value * fraction;
        return (float) randomFloat(value - radius, value + radius);
    }

    public static float frameSeconds(long previousNanos) {
        long now = System.nanoTime();
        float seconds = previousNanos == 0L ? 0.016666668F : (float) (now - previousNanos) / 1.0E9F;
        return Math.min(seconds, 0.1F);
    }

    public static long nanoTime() {
        return System.nanoTime();
    }
}
