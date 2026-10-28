package dev.echo.client.config;

import dev.echo.client.features.Feature;
import dev.echo.client.input.BindMode;
import dev.echo.client.input.InputKey;
import java.util.Objects;

/**
 * The current feature state and its keyboard or mouse binding.
 *
 * @param feature the feature instance
 * @param id the stable feature identifier
 * @param label the display label
 * @param enabled whether the feature is enabled
 * @param binding the current binding
 * @param mode the toggle or hold mode
 * @param pressed whether the binding is currently pressed
 */
public record FeatureBindSnapshot(Feature feature, String id, CharSequence label,
                                  boolean enabled, InputKey binding, BindMode mode, boolean pressed) {
    /** Validates required components in the original constructor order. */
    public FeatureBindSnapshot {
        Objects.requireNonNull(feature, "module");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(binding, "binding");
        Objects.requireNonNull(mode, "mode");
    }
}
