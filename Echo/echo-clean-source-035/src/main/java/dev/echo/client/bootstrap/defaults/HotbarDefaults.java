package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.HotbarFeature;

/** Constructs, assigns, collects and post-initializes Hotbar. */
public final class HotbarDefaults {
    private HotbarDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(HotbarFeature feature) {


        feature.settings.clear();
        dev.echo.client.features.hud.EchoHudElement.appendBootstrapSettings(feature);
        dev.echo.client.features.HudFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
