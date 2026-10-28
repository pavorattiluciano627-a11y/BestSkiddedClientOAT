package dev.echo.client.handlers;

/** Applies the client pitch limits without canonicalizing NaNs or signed zero. */
public final class PitchLimits {
    private PitchLimits() {
    }

    /**
     * Clamps a pitch to the inclusive range [-90, 90] degrees.
     *
     * @param pitch input angle in degrees
     * @return the bounded angle, or the original NaN payload
     */
    public static float clampDegrees(float pitch) {
        // Operand ordering matches the native MAXSS/MINSS instructions.
        float lowerClamped = -90.0F > pitch ? -90.0F : pitch;
        return 90.0F < lowerClamped ? 90.0F : lowerClamped;
    }
}
