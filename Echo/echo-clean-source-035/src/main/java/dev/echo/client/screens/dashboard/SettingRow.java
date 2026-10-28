package dev.echo.client.screens.dashboard;

import dev.echo.client.setting.Setting;

/** Original positioned setting and its logical column. */
public record SettingRow(Setting setting, float x, float y, float height, int column) {
}
