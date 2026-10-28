package dev.echo.client.utils.trajectory;

/**
 * Candidate record for the result of the trajectory regression.
 * Accessor dispatch remains pending; see needs.json. This is not a 3D vector.
 */
public record LinearFit(double intercept, double slope, double meanSquaredError) {
}
