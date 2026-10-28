package dev.echo.client.feature.movement;

import java.util.function.BooleanSupplier;

/** A queued inventory operation and the condition confirming its completion.
 * @param action operation to execute once
 * @param ready completion condition checked on later ticks
 */
public record DiveBombAction(Runnable action, BooleanSupplier ready) {
}
