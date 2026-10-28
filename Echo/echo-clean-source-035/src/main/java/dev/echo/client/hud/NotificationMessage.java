package dev.echo.client.hud;

/** Notification identity, immutable payload reference and original icon identity. */
public record NotificationMessage(long id, CharSequence text, HudIcon icon) {
}
