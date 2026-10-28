package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.KeybindsFeature;

/** Constructs, assigns, collects and post-initializes Keybinds. */
public final class KeybindsDefaults {
    private KeybindsDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(KeybindsFeature feature) {


        feature.settings.clear();
        dev.echo.client.features.hud.EchoHudElement.appendBootstrapSettings(feature);
        dev.echo.client.features.HudFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
