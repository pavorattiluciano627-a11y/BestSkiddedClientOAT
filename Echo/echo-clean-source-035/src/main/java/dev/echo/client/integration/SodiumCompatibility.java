package dev.echo.client.integration;

import net.fabricmc.loader.api.FabricLoader;

/** Lazily caches whether the Sodium renderer is present in the Fabric mod set. */
public final class SodiumCompatibility {
    private static Boolean loaded;

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = FabricLoader.getInstance().isModLoaded("sodium");
        }
        return loaded;
    }
}
