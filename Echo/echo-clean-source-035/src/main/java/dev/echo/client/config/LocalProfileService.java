package dev.echo.client.config;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Provides local profile operations with explicit storage ownership.
 *
 * <p>Use the path constructor for isolated stores. The client singleton uses
 * Fabric's configuration directory only when {@link #forClient()} is explicitly
 * requested; creating or testing an isolated service does not initialize it.
 */
public final class LocalProfileService {
    private final ProfileStore store;

    /**
     * Creates a service for an explicitly supplied directory, without filesystem I/O.
     *
     * @param directory the directory containing profiles.json and its lock/backup files
     */
    public LocalProfileService(Path directory) {
        store = new ProfileStore(directory);
    }

    /**
     * Returns the shared service for Fabric's {@code config/echo} directory.
     *
     * @return the lazily initialized client service
     */
    public static LocalProfileService forClient() {
        return ClientInstance.SERVICE;
    }

    /**
     * Reports local protocol support without testing directory access.
     *
     * @return true for the supported local implementation
     */
    public boolean isAvailable() {
        return store.isAvailable();
    }

    /**
     * Lists local profile metadata and the current selection.
     *
     * @return the unchanged JSON protocol response
     */
    public JsonObject listProfiles() {
        return store.listProfiles();
    }

    /**
     * Reads a profile, using the active profile when the ID is empty.
     *
     * @param id the profile ID, or null/empty for the active profile
     * @return the unchanged JSON protocol response
     */
    public JsonObject getProfile(String id) {
        return store.getProfile(id);
    }

    /**
     * Creates or updates a profile.
     *
     * @param request the explicit six-field save request
     * @return the unchanged JSON protocol response
     */
    public JsonObject saveProfile(ProfileStore.SaveRequest request) {
        return store.saveProfile(request);
    }

    /**
     * Selects a profile, or clears the selection when the ID is empty.
     *
     * @param id the profile ID, or null/empty to clear the selection
     * @return the unchanged JSON protocol response
     */
    public JsonObject selectProfile(String id) {
        return store.selectProfile(id);
    }

    /**
     * Deletes a stored profile.
     *
     * @param id the existing profile ID
     * @return the unchanged JSON protocol response
     */
    public JsonObject deleteProfile(String id) {
        return store.deleteProfile(id);
    }

    /**
     * Preserves the human-readable local operation facade for external callers.
     *
     * @param operation one of the public local operation names
     * @param arguments the original operation arguments
     * @return literal availability or pretty-printed JSON protocol results
     * @see ProfileStore#executeOperation(String, String[])
     */
    public String executeOperation(String operation, String[] arguments) {
        return store.executeOperation(operation, arguments);
    }

    private static final class ClientInstance {
        private static final LocalProfileService SERVICE = new LocalProfileService(
                FabricLoader.getInstance().getConfigDir().resolve("echo"));
    }
}
