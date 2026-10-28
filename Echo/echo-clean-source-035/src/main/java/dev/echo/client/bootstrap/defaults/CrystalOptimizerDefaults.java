package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.CrystalOptimizerFeature;

/** Constructs, assigns, collects and post-initializes Crystal Optimizer. */
public final class CrystalOptimizerDefaults {
    private CrystalOptimizerDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(CrystalOptimizerFeature feature) {


        feature.settings.clear();
        feature.postInitialize();
    }
}
