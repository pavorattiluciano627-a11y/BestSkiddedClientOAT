package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.ArmorStatusFeature;

/** Constructs, assigns, collects and post-initializes Armor Status. */
public final class ArmorStatusDefaults {
    private ArmorStatusDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(ArmorStatusFeature feature) {


        feature.settings.clear();
        dev.echo.client.features.hud.EchoHudElement.appendBootstrapSettings(feature);
        dev.echo.client.features.HudFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
