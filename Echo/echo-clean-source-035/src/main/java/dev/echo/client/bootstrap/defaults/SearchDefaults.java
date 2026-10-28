package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.SearchFeature;

/** Constructs, assigns, collects and post-initializes Search. */
public final class SearchDefaults {
    private SearchDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(SearchFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedTracers = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Tracers"), false);
        dev.echo.client.setting.BooleanSetting preparedOutline = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Outline"), true);
        dev.echo.client.setting.FloatSetting preparedOpacity = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Opacity"), 0.4f, 0.0f, 1.0f, 0.05f);

        SearchFeature.assignBootstrapSettings(feature, preparedTracers, preparedOutline, preparedOpacity);

        feature.settings.clear();
        dev.echo.client.features.impl.render.SearchFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
