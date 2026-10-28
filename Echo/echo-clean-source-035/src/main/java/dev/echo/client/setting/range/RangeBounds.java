package dev.echo.client.setting.range;

/** Shared label, bounds, step and display unit; construction does not normalize them. */
public record RangeBounds(CharSequence label, float minimum, float maximum,
                          float step, CharSequence unit) {
}
