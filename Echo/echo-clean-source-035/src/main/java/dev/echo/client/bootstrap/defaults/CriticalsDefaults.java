package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.CriticalsFeature;

/** Constructs, assigns, collects and post-initializes Criticals. */
public final class CriticalsDefaults {
    private CriticalsDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(CriticalsFeature feature) {
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Unsprint"), new java.lang.CharSequence[] {CatalogText.literal("Vanilla"), CatalogText.literal("Unsprint"), CatalogText.literal("Release")});
        dev.echo.client.setting.ChoiceSetting preparedResume = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Resume"), CatalogText.literal("Instant"), new java.lang.CharSequence[] {CatalogText.literal("Instant"), CatalogText.literal("Delay"), CatalogText.literal("Ground")});
        dev.echo.client.setting.FloatRangeSetting preparedResumeDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Resume Delay"), 1.0f, 3.0f, 1.0f, 10.0f, 1.0f, CatalogText.literal("t"));

        CriticalsFeature.assignBootstrapSettings(feature, preparedMode, preparedResume, preparedResumeDelay);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.CriticalsFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
