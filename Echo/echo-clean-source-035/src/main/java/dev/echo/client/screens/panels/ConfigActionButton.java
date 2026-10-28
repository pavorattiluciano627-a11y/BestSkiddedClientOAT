package dev.echo.client.screens.panels;

/** A configuration action and its last rendered hit rectangle. */
final class ConfigActionButton {
    final String configName;
    final ConfigAction action;
    final float x;
    final float y;
    final float width;
    final float height;

    ConfigActionButton(String configName, ConfigAction action,
                       float x, float y, float width, float height) {
        this.configName = configName;
        this.action = action;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}
