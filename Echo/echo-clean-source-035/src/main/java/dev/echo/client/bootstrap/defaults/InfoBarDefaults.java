package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.InfoBarFeature;

/** Constructs, assigns, collects and post-initializes Info Bar. */
public final class InfoBarDefaults {
    private InfoBarDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(InfoBarFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedShowFrameTime = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Show Frame Time"), false);
        dev.echo.client.setting.BooleanSetting preparedShowFPS = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Show FPS"), true);
        dev.echo.client.setting.BooleanSetting preparedShowPing = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Show Ping"), false);
        dev.echo.client.setting.BooleanSetting preparedShowUsername = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Show Username"), true);

        InfoBarFeature.assignBootstrapSettings(feature, preparedShowFrameTime, preparedShowFPS, preparedShowPing, preparedShowUsername);

        feature.settings.clear();
        dev.echo.client.features.impl.render.InfoBarFeature.appendBootstrapSettings(feature);
        dev.echo.client.features.hud.EchoHudElement.appendBootstrapSettings(feature);
        dev.echo.client.features.HudFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
