package dev.echo.client.config;

import dev.echo.client.features.Category;
import java.util.Objects;
import java.util.Optional;

/**
 * An enabled-feature row with an optional key label.
 *
 * @param id the stable feature identifier
 * @param name the display name
 * @param category the feature category, which may be null
 * @param keyLabel the optional key label
 * @param pressed whether the key is currently pressed
 */
public record FeatureSnapshotDiff(String id, CharSequence name, Category category,
                                  Optional<String> keyLabel, boolean pressed) {
    /** Validates the three required record components in their original order. */
    public FeatureSnapshotDiff {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(keyLabel, "keyLabel");
    }
}
