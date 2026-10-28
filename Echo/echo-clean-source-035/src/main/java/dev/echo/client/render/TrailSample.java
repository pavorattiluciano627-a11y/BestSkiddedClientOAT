package dev.echo.client.render;

/** A height/rotation sample of the animated ring around the selected entity. */
public record TrailSample(double heightOffset, double angleRadians, long createdMillis) {
}
