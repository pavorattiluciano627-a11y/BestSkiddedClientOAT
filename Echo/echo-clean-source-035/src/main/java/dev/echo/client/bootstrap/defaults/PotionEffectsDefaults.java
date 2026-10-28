package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.PotionEffectsFeature;

/** Constructs, assigns, collects and post-initializes Potion Effects. */
public final class PotionEffectsDefaults {
    private PotionEffectsDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(PotionEffectsFeature feature) {


        feature.settings.clear();
        dev.echo.client.features.hud.EchoHudElement.appendBootstrapSettings(feature);
        dev.echo.client.features.HudFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
