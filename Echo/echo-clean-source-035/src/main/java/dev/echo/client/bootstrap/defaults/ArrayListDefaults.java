package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.ArrayListFeature;

/** Constructs, assigns, collects and post-initializes Array List. */
public final class ArrayListDefaults {
    private ArrayListDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(ArrayListFeature feature) {
        dev.echo.client.setting.ChoiceSetting preparedVisibilityFilter = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Visibility filter"), CatalogText.literal("Blacklist"), new java.lang.CharSequence[] {CatalogText.literal("Blacklist"), CatalogText.literal("Whitelist")});
        dev.echo.client.setting.BooleanSetting preparedLowercase = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Lowercase"), false);
        dev.echo.client.setting.BooleanSetting preparedSuffixEnabled = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Suffix Enabled"), true);

        ArrayListFeature.assignBootstrapSettings(feature, preparedVisibilityFilter, preparedLowercase, preparedSuffixEnabled);

        feature.settings.clear();
        dev.echo.client.features.impl.render.ArrayListFeature.appendBootstrapSettings(feature);
        dev.echo.client.features.hud.EchoHudElement.appendBootstrapSettings(feature);
        dev.echo.client.features.HudFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
