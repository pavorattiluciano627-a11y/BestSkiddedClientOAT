package dev.echo.client.input;

/**
 * A temporary key state and its absolute expiration in the nano-time domain.
 *
 * @param pressed the override's pressed state
 * @param expiresAtNanos the inclusive expiration deadline, not the creation time
 */
record KeyOverride(boolean pressed, long expiresAtNanos) {
}
