package dev.echo.client.utils.simulation;

/** Candidate interval for checking two simulation samples; not movement input axes. */
public record TickWindow(int firstTick, int lastTick, int pairedTickOffset) {
}
