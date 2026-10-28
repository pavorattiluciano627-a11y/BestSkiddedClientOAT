package dev.echo.client.screens.dashboard;

import dev.echo.client.setting.Setting;
import java.util.List;

/** Original title and live ordered setting-list pair. */
public record SettingSection(CharSequence title, List<Setting> settings) {
}
