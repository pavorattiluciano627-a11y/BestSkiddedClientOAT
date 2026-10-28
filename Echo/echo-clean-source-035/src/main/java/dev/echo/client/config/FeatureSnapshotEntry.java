package dev.echo.client.config;

import dev.echo.client.features.Category;
import java.util.Objects;

/**
 * A feature or setting row in the keybind display.
 *
 * @param id the stable row identifier
 * @param name the display name
 * @param category the feature category, which may be null
 * @param keyLabel the key label, or a dash for an unbound feature
 * @param enabled whether the feature is enabled
 * @param pressed whether the key is currently pressed
 */
public record FeatureSnapshotEntry(String id, CharSequence name, Category category,
                                   String keyLabel, boolean enabled, boolean pressed) {
    /** Validates the three required record components in their original order. */
    public FeatureSnapshotEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(keyLabel, "keyLabel");
    }
}
