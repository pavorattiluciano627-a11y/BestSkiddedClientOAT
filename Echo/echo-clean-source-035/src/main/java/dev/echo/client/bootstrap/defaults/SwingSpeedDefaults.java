package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.SwingSpeedFeature;

/** Constructs, assigns, collects and post-initializes Swing Speed. */
public final class SwingSpeedDefaults {
    private SwingSpeedDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(SwingSpeedFeature feature) {
        dev.echo.client.setting.IntSetting preparedSwingSpeed = new dev.echo.client.setting.IntSetting(CatalogText.literal("Swing Speed"), 7, 1, 20, CatalogText.literal(" ticks"));

        SwingSpeedFeature.swingSpeed = preparedSwingSpeed;

        feature.settings.clear();
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.SwingSpeedFeature.swingSpeed);
        feature.postInitialize();
    }
}
