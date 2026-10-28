package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.OffhandFeature;

/** Constructs, assigns, collects and post-initializes Offhand. */
public final class OffhandDefaults {
    private OffhandDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(OffhandFeature feature) {


        feature.settings.clear();
        dev.echo.client.features.hud.EchoHudElement.appendBootstrapSettings(feature);
        dev.echo.client.features.HudFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
