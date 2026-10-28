package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.MusicFeature;

/** Constructs, assigns, collects and post-initializes Music. */
public final class MusicDefaults {
    private MusicDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(MusicFeature feature) {


        feature.settings.clear();
        dev.echo.client.features.hud.EchoHudElement.appendBootstrapSettings(feature);
        dev.echo.client.features.HudFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
