package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.internals.SilentAimFeature;

/** Constructs, assigns, collects and post-initializes Silent Aim. */
public final class SilentAimDefaults {
    private SilentAimDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(SilentAimFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedRenderRotations = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Render Rotations"), true);
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), dev.echo.client.features.internals.SilentAimFeature.bootstrapSilentMode(), new java.lang.CharSequence[] {dev.echo.client.features.internals.SilentAimFeature.bootstrapNoneMode(), dev.echo.client.features.internals.SilentAimFeature.bootstrapSilentMode(), dev.echo.client.features.internals.SilentAimFeature.bootstrapStrictMode()});

        SilentAimFeature.assignBootstrapSettings(feature, preparedRenderRotations, preparedMode);

        feature.settings.clear();
        dev.echo.client.features.internals.SilentAimFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
