package dev.echo.client.bootstrap;

import dev.echo.client.features.Feature;
import dev.echo.client.setting.Setting;

/** Implements the original value test and equals-based duplicate suppression. */
public final class CatalogSettings {
    private CatalogSettings() {
    }

    /**
     * Adds one just-read field value without batching reads or dispatching a collector.
     * @param feature the feature whose live settings list receives the value
     * @param value the value read directly from its declaring class
     */
    public static void append(Feature feature, Object value) {
        if (value instanceof Setting setting && !feature.settings.contains(setting)) {
            feature.settings.add(setting);
        }
    }
}
