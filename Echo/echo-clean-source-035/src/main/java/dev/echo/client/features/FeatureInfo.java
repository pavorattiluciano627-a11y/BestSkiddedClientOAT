package dev.echo.client.features;

import dev.echo.client.input.InputKey;

/** The five original feature metadata components and their constructor defaults. */
public record FeatureInfo(CharSequence name, CharSequence description, Category category,
                          InputKey key, boolean visible) {
    public FeatureInfo {
        // The original canonical constructor calls Feature's static state reader.
        // Preserve its meaningful class initialization without retaining that guard.
        try {
            Class.forName(Feature.class.getName(), true, Feature.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        name = name == null ? "" : name;
        description = description == null ? "" : description;
        category = category == null ? Category.INTERNALS : category;
        key = key == null ? InputKey.NONE : key;
    }

    public FeatureInfo(CharSequence name, CharSequence description, Category category, InputKey key) {
        this(name, description, category, key, true);
    }

    public FeatureInfo(CharSequence name, CharSequence description, Category category,
                       int keyCode, boolean visible) {
        this(name, description, category, InputKey.fromCode(keyCode), visible);
    }

    public FeatureInfo(CharSequence name, CharSequence description, Category category, int keyCode) {
        this(name, description, category, keyCode, true);
    }

    public FeatureInfo(CharSequence name, CharSequence description, Category category, boolean visible) {
        this(name, description, category, InputKey.NONE, visible);
    }

    public FeatureInfo(CharSequence name, CharSequence description, Category category) {
        this(name, description, category, InputKey.NONE, true);
    }
}
