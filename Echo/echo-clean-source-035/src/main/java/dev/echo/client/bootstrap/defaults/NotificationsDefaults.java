package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.NotificationsFeature;

/** Constructs, assigns, collects and post-initializes Notifications. */
public final class NotificationsDefaults {
    private NotificationsDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(NotificationsFeature feature) {
        dev.echo.client.setting.FloatSetting preparedDuration = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Duration"), 1.5f, 0.5f, 8.0f, 0.1f, new String("s"));

        NotificationsFeature.assignBootstrapSettings(feature, preparedDuration);

        feature.settings.clear();
        dev.echo.client.features.impl.render.NotificationsFeature.appendBootstrapSettings(feature);
        dev.echo.client.features.hud.EchoHudElement.appendBootstrapSettings(feature);
        dev.echo.client.features.HudFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
