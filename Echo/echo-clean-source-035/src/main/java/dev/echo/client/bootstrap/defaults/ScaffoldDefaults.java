package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.world.ScaffoldFeature;

/** Constructs, assigns, collects and post-initializes Scaffold. */
public final class ScaffoldDefaults {
    private ScaffoldDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(ScaffoldFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedPitchCheck = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Pitch Check"), true);
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), dev.echo.client.features.impl.world.ScaffoldFeature.bootstrapModeOption(), new java.lang.CharSequence[] {dev.echo.client.features.impl.world.ScaffoldFeature.bootstrapModeOption()});
        dev.echo.client.setting.ChoiceSetting preparedListType = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("List Type"), dev.echo.client.features.impl.world.ScaffoldFeature.bootstrapListTypeOption(), new java.lang.CharSequence[] {dev.echo.client.features.impl.world.ScaffoldFeature.bootstrapListTypeOption(), dev.echo.client.features.impl.world.ScaffoldFeature.bootstrapListTypeOption2()});

        ScaffoldFeature.assignBootstrapSettings(feature, preparedPitchCheck, preparedMode, preparedListType);

        feature.settings.clear();
        dev.echo.client.features.impl.world.ScaffoldFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
