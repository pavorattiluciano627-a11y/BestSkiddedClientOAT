package dev.echo.client.hud;

/** Original module/keybind row; component references and ordering are retained. */
public record KeybindRow(String id, CharSequence name, CharSequence detail,
                         String binding, HudIcon icon, boolean enabled) {
}
