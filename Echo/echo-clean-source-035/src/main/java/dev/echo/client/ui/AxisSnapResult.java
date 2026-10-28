package dev.echo.client.ui;

/**
 * Snap-state hysteresis result, including entry and exit edges.
 * Source-complete candidate: the original accessor bindings retain their applicability obligation.
 *
 * @param snapped current state
 * @param entered newly entered the snapped state
 * @param exited newly exited the snapped state
 */
public record AxisSnapResult(boolean snapped, boolean entered, boolean exited) {
}
