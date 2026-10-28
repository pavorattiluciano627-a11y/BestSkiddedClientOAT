package dev.echo.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Maintains the configuration UI's metadata cache above the root-owned store.
 *
 * <p>Snapshot capture/application belongs to the feature configuration layer.
 * This class accepts already serialized snapshot data and never reads client
 * identity, worlds or an implicit profile directory.
 */
public final class ProfileCatalog {
    private final ProfileIoExecutor io;
    private volatile String activeConfigId = "";
    private volatile boolean available;
    private volatile List<ConfigMetadata> cachedProfiles = List.of();

    /**
     * Creates an empty metadata cache without reading storage.
     *
     * @param io the shared configuration I/O queue
     */
    public ProfileCatalog(ProfileIoExecutor io) {
        this.io = Objects.requireNonNull(io, "io");
    }

    /**
     * Returns the last active identifier returned by a successful response.
     *
     * @return the selected identifier, or an empty string
     */
    public String activeConfigId() {
        return activeConfigId;
    }

    /** Publishes the active identifier from a successful select/get response. */
    void acceptActiveConfigId(String id) {
        activeConfigId = id;
    }

    /**
     * Returns the availability result from the most recent refresh.
     *
     * @return the cached availability state
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Refreshes metadata when the local protocol is available.
     *
     * <p>A failed list response retains the previous immutable cache. A successful
     * response returns a newly mutable list and stores an immutable copy, matching
     * the original distinction between fresh and cached results.
     *
     * @return the profiles, or an empty list when the protocol is unavailable
     */
    public List<ConfigMetadata> refresh() {
        if (!io.isAvailable()) {
            available = false;
            return List.of();
        }
        List<ConfigMetadata> result = readProfiles();
        available = true;
        return result;
    }

    /**
     * Refreshes and returns profile names in server order, including duplicates.
     *
     * @return the display names
     */
    public List<String> names() {
        List<String> names = new ArrayList<>();
        for (ConfigMetadata profile : refresh()) {
            names.add(profile.name());
        }
        return names;
    }

    private List<ConfigMetadata> readProfiles() {
        JsonObject response = io.listProfiles();
        if (!ProfileResponseCodec.isSuccessful(response)) {
            return cachedProfiles;
        }
        activeConfigId = ProfileResponseCodec.textField(response, "activeConfigId");
        JsonArray configs = response.has("configs") && response.get("configs").isJsonArray()
                ? response.getAsJsonArray("configs") : new JsonArray();
        List<ConfigMetadata> profiles = new ArrayList<>();
        for (JsonElement config : configs) {
            if (config.isJsonObject()) {
                profiles.add(ProfileResponseCodec.metadata(config.getAsJsonObject()));
            }
        }
        cachedProfiles = List.copyOf(profiles);
        return profiles;
    }

    /**
     * Finds the first profile selected by identifier or by its active flag.
     *
     * @return the active metadata, or null
     */
    public ConfigMetadata findActive() {
        for (ConfigMetadata profile : readProfiles()) {
            if (profile.id().equals(activeConfigId) || profile.active()) {
                return profile;
            }
        }
        return null;
    }

    /**
     * Finds a name case-insensitively, preferring owned, active, then newer profiles.
     *
     * @param name the exact untrimmed display name to match
     * @param requirePrivateOwned whether public or unowned profiles must be excluded
     * @return the preferred match, or null
     */
    public ConfigMetadata findByName(String name, boolean requirePrivateOwned) {
        if (name == null || name.isBlank()) {
            return null;
        }
        List<ConfigMetadata> matches = new ArrayList<>();
        for (ConfigMetadata profile : refresh()) {
            if (!profile.name().equalsIgnoreCase(name)) {
                continue;
            }
            if (requirePrivateOwned && (!profile.owned() || profile.publicVisibility())) {
                continue;
            }
            matches.add(profile);
        }
        if (matches.isEmpty()) {
            return null;
        }
        matches.sort(Comparator.comparing(ProfileCatalog::isUnowned)
                .thenComparing(ProfileCatalog::isInactive)
                .thenComparing(ConfigMetadata::updatedAt, Comparator.reverseOrder()));
        return matches.getFirst();
    }

    private static Boolean isUnowned(ConfigMetadata profile) {
        return !profile.owned();
    }

    private static Boolean isInactive(ConfigMetadata profile) {
        return !profile.active();
    }

    /**
     * Saves serialized snapshot data and decodes the returned metadata.
     *
     * @param id the existing identifier, or empty to create
     * @param name the profile name
     * @param description the description, with null interpreted as empty
     * @param publicVisibility whether the profile should be public
     * @param type the type, with null interpreted as the original GHOST value
     * @param data already serialized snapshot data
     * @return the saved metadata, or null for a failure or missing metadata
     */
    public ConfigMetadata save(String id, String name, String description,
                               boolean publicVisibility, String type, String data) {
        JsonObject response = io.saveProfile(new ProfileStore.SaveRequest(
                id == null ? "" : id, name, description == null ? "" : description,
                publicVisibility ? "PUBLIC" : "PRIVATE", type == null ? "GHOST" : type, data));
        if (!ProfileResponseCodec.isSuccessful(response)) {
            return null;
        }
        activeConfigId = ProfileResponseCodec.textField(response, "activeConfigId");
        JsonObject metadata = ProfileResponseCodec.objectField(response, "config");
        return metadata == null ? null : ProfileResponseCodec.metadata(metadata);
    }

    /**
     * Saves a named private snapshot, preserving the original autosave selection rule.
     *
     * @param name the display name, or the compatibility identifier {@code _autosave}
     * @param data already serialized snapshot data
     * @return whether metadata was returned by the save operation
     */
    public boolean saveSnapshot(String name, String data) {
        if (!io.isAvailable()) {
            return false;
        }
        boolean autosave = "_autosave".equals(name);
        ConfigMetadata existing = autosave ? findActive() : findByName(name, true);
        String id = existing == null ? "" : existing.id();
        String displayName = autosave ? (existing == null ? "Default" : existing.name()) : name;
        String description = existing == null ? "" : existing.description();
        String type = existing == null ? "GHOST" : existing.type();
        return save(id, displayName, description, false, type, data) != null;
    }
}
