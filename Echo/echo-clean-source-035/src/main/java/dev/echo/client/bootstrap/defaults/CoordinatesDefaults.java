package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.CoordinatesFeature;

/** Constructs, assigns, collects and post-initializes Coordinates. */
public final class CoordinatesDefaults {
    private CoordinatesDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(CoordinatesFeature feature) {


        feature.settings.clear();
        dev.echo.client.features.hud.EchoHudElement.appendBootstrapSettings(feature);
        dev.echo.client.features.HudFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
