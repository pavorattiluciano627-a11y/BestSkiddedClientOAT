package dev.echo.client.setting.range;

/** A labelled interval drawn alongside a named range group. */
public record RangeBand(CharSequence label, float lower, float upper) {
}
