package dev.echo.client.bootstrap;

import net.fabricmc.api.ModInitializer;

/** Fabric main entrypoint for the complete source-owned client bootstrap graph. */
public final class EchoInitializer implements ModInitializer {
    /** Constructs the graph once; ClientBootstrap invokes Echo's final hook last. */
    @Override
    public void onInitialize() {
        ClientBootstrap.initializeCatalog();
    }
}
