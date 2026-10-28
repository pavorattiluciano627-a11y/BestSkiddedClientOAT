package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.internals.ClickGUIFeature;

/** Constructs, assigns, collects and post-initializes Click GUI. */
public final class ClickGUIDefaults {
    private ClickGUIDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(ClickGUIFeature feature) {
        dev.echo.client.setting.FloatSetting preparedScrollSpeed = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Scroll Speed"), 50.0f, 1.0f, 50.0f, 1.0f);
        dev.echo.client.setting.BooleanSetting preparedStreamProof = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Stream proof"), false);

        ClickGUIFeature.scrollSpeed = preparedScrollSpeed;
        ClickGUIFeature.streamProof = preparedStreamProof;

        feature.settings.clear();
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.internals.ClickGUIFeature.scrollSpeed);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.internals.ClickGUIFeature.streamProof);
        feature.postInitialize();
    }
}
