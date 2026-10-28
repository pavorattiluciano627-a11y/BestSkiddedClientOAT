package dev.echo.client.particle;

/**
 * Normalized bounds of one particle image within the particle texture atlas.
 * Fixed-join field bindings still require the shared-cache applicability proof.
 */
public record ParticleUv(float minU, float minV, float maxU, float maxV) {
}
