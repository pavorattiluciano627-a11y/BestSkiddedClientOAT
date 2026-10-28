package dev.echo.client;

import dev.echo.client.bootstrap.ClientBootstrap;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/** Establishes the supplied client's offline runtime session before service construction. */
public final class EchoPreLaunch implements PreLaunchEntrypoint {
    /** Publishes the same session consumed by entitlement queries and runtime commands. */
    @Override
    public void onPreLaunch() {
        ClientBootstrap.prepare();
    }
}
