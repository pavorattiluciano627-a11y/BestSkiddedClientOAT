package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.internals.TransactionsFeature;

/** Constructs, assigns, collects and post-initializes Transactions. */
public final class TransactionsDefaults {
    private TransactionsDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(TransactionsFeature feature) {


        feature.settings.clear();
        feature.postInitialize();
    }
}
