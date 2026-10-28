package dev.echo.client.ui.animation;

/** The original cubic coordinate and derivative used by the tween solver. */
public final class CubicBezier {
    private CubicBezier() {
    }

    /**
     * Evaluates a cubic coordinate with fixed endpoints zero and one.
     *
     * @param parameter the curve parameter; it is not clamped
     * @param firstControl the first control coordinate
     * @param secondControl the second control coordinate
     * @return the coordinate, in the original single-precision evaluation order
     */
    public static float coordinate(float parameter, float firstControl, float secondControl) {
        float complement = 1.0F - parameter;
        return 3.0F * complement * complement * parameter * firstControl
                + 3.0F * complement * parameter * parameter * secondControl
                + parameter * parameter * parameter;
    }

    /**
     * Evaluates the derivative of {@link #coordinate(float, float, float)}.
     *
     * @param parameter the curve parameter; it is not clamped
     * @param firstControl the first control coordinate
     * @param secondControl the second control coordinate
     * @return the derivative, in the original single-precision evaluation order
     */
    public static float derivative(float parameter, float firstControl, float secondControl) {
        float complement = 1.0F - parameter;
        return 3.0F * complement * complement * firstControl
                + 6.0F * complement * parameter * (secondControl - firstControl)
                + 3.0F * parameter * parameter * (1.0F - secondControl);
    }
}
