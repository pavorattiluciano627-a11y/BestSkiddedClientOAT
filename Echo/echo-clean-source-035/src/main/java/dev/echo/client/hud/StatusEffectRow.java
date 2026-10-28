package dev.echo.client.hud;

/** Original effect name, amplifier suffix, time text, duration fraction and icon. */
public record StatusEffectRow(String name, String amplifier, String duration,
                              float remainingFraction, HudIcon icon) {
}
