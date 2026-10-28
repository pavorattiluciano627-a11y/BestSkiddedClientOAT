package dev.echo.client.setting.ui;

import dev.echo.client.hud.ItemHudLayout;

/** Drawing operation receiving the UI-owned item/HUD rendering context. */
@FunctionalInterface
public interface SettingWidgetRenderer {
    void render(ItemHudLayout context);
}
