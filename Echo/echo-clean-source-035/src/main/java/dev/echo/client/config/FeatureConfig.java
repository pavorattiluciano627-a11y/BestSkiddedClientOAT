package dev.echo.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.echo.client.Echo;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureManager;
import dev.echo.client.features.internals.ClickGUIFeature;
import dev.echo.client.friends.FriendManager;
import dev.echo.client.setting.Setting;
import dev.echo.client.utils.strings.Concat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Captures and applies the feature-profile format and coordinates profile actions.
 *
 * <p>Feature names and setting keys are content hashes. Setting values use their
 * original typed encodings and setters; enablement is applied before settings.
 * Local file persistence remains owned by {@link LocalProfileService}.
 */
public final class FeatureConfig implements AutoCloseable {
    private final Gson json = new GsonBuilder().setPrettyPrinting().create();
    private final ClientState clientState;
    private final ProfileIoExecutor io;
    private final ProfileCatalog catalog;
    private boolean preserveClickGuiBindings;

    /** Shared load/deferred-initialization flag also used by the client reloader. */
    public boolean loading;

    /**
     * Supplies genuine live service references and the original Click GUI type test.
     * Capture, conversion, ordering and application are implemented by this class.
     */
    public interface ClientState {
        /** Returns the current feature manager, which may be absent during startup. */
        FeatureManager features();
        /** Returns the current friend manager, which may be absent during startup. */
        FriendManager friends();
        /** Returns the current server entry, or null for singleplayer/no connection. */
        ServerData server();
        /** Returns the client's lifecycle write gate. */
        boolean writesDisabled();
        /** Performs the original Click GUI feature subtype check. */
        boolean isClickGui(Feature feature);
    }

    /** Constructs the live client provider without reading a profile file. */
    public FeatureConfig() {
        this(LocalProfileService.forClient(), new LiveClientState());
    }

    /**
     * Constructs a provider with explicit service ownership, suitable for isolation.
     *
     * @param profiles the root-owned profile service
     * @param clientState the live client services or isolated synthetic equivalents
     */
    public FeatureConfig(LocalProfileService profiles, ClientState clientState) {
        this.clientState = Objects.requireNonNull(clientState, "clientState");
        io = new ProfileIoExecutor(profiles);
        catalog = new ProfileCatalog(io);
    }

    /** Returns the last successful response's active profile identifier. */
    public String getActiveConfigId() {
        return catalog.activeConfigId();
    }

    /** Returns availability as last recorded by a profile-list refresh. */
    public boolean isAvailable() {
        return catalog.isAvailable();
    }

    /** Refreshes the metadata list, retaining its original cached-failure behavior. */
    public List<ConfigMetadata> list() {
        return catalog.refresh();
    }

    /** Lists profile names in response order, preserving duplicate names. */
    public List<String> names() {
        return catalog.names();
    }

    /** Refreshes metadata on the serial configuration I/O worker. */
    public CompletableFuture<List<ConfigMetadata>> listAsync() {
        return io.submit(this::list);
    }

    /**
     * Captures current feature values, server metadata and a copy of friend names.
     *
     * @return a newly allocated snapshot tree with the original insertion order
     */
    public JsonObject capture() {
        JsonObject snapshot = new JsonObject();
        JsonArray features = new JsonArray();
        if (clientState.features() == null || clientState.features().getFeatures() == null) {
            snapshot.add("settings", features);
            snapshot.add("friends", new JsonArray());
            snapshot.add("server", new JsonObject());
            return snapshot;
        }
        List<Feature> current = clientState.features().getFeatures();
        for (Feature feature : current) {
            if (!feature.isPersistent()) {
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("name", Concat.shortHash(feature.getName()));
            entry.addProperty("enabled", feature.persistsEnabledState() && feature.isEnabled());
            entry.addProperty("keybind", feature.getKey().toString());
            entry.addProperty("hold", feature.isHoldMode());
            entry.addProperty("vis", feature.isVisible());
            if (feature.hasSettings()) {
                JsonObject settings = new JsonObject();
                Set<String> usedKeys = new HashSet<>();
                for (Setting setting : feature.settings) {
                    // The original key calculation is outside the per-setting catch.
                    String key = FeatureSettingCodec.uniqueKey(setting, usedKeys);
                    try {
                        JsonElement value = FeatureSettingCodec.encode(setting);
                        if (value != null) {
                            settings.add(key, value);
                        }
                    } catch (Exception failure) {
                        System.err.println("Failed to save setting hash: " + Concat.shortHash(setting.getLabel()));
                        failure.printStackTrace();
                    }
                }
                if (!settings.entrySet().isEmpty()) {
                    entry.add("settings", settings);
                }
            }
            features.add(entry);
        }
        captureServer(snapshot);
        captureFriends(snapshot);
        snapshot.add("settings", features);
        return snapshot;
    }

    /** Serializes a fresh snapshot with the original pretty-printing Gson settings. */
    public String serialize() {
        return json.toJson(capture());
    }

    /**
     * Applies a snapshot in feature/setting order, then replaces the friend list.
     *
     * <p>Malformed feature-level structure and feature-hook failures propagate.
     * A malformed individual setting is logged and the next setting is processed.
     * Server metadata is descriptive and is never used to connect to a server.
     *
     * @param snapshot the decoded profile snapshot
     * @return false when the root or its settings member is absent, true otherwise
     */
    public boolean apply(JsonObject snapshot) {
        if (snapshot == null || !snapshot.has("settings")) {
            return false;
        }
        JsonArray entries = snapshot.getAsJsonArray("settings");
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            String nameHash = ProfileResponseCodec.textField(entry, "name");
            Feature feature = findFeature(nameHash);
            if (feature == null || !feature.isPersistent()) {
                continue;
            }
            if (entry.has("enabled") && feature.persistsEnabledState()) {
                feature.setEnabled(entry.get("enabled").getAsBoolean());
            }
            boolean keepBinding = preserveClickGuiBindings && clientState.isClickGui(feature);
            if (!keepBinding && entry.has("keybind")) {
                feature.setKey(FeatureSettingCodec.readKey(entry.get("keybind")));
            }
            if (!keepBinding && entry.has("hold")) {
                feature.setHoldMode(entry.get("hold").getAsBoolean());
            }
            if (entry.has("vis")) {
                feature.setVisible(entry.get("vis").getAsBoolean());
            }
            if (!entry.has("settings")) {
                continue;
            }
            JsonObject settings = entry.getAsJsonObject("settings");
            Set<String> usedKeys = new HashSet<>();
            for (Setting setting : feature.settings) {
                String key = FeatureSettingCodec.uniqueKey(setting, usedKeys);
                if (!settings.has(key)) {
                    continue;
                }
                try {
                    FeatureSettingCodec.apply(setting, settings.get(key));
                } catch (Exception failure) {
                    System.err.println("Failed to load setting hash: " + Concat.shortHash(setting.getLabel()));
                    failure.printStackTrace();
                }
            }
        }
        applyFriends(snapshot);
        preserveClickGuiBindings = true;
        return true;
    }

    /**
     * Parses and applies data while publishing the loading flag.
     *
     * @param data serialized snapshot data
     * @return the application result, or false after an ordinary parse/apply failure
     */
    public boolean loadSnapshot(String data) {
        loading = true;
        try {
            return apply(json.fromJson(data, JsonObject.class));
        } catch (Exception failure) {
            failure.printStackTrace();
            return false;
        } finally {
            loading = false;
        }
    }

    /** Loads a named profile, or the currently active profile for {@code _autosave}. */
    public boolean load(String name) {
        if (!io.isAvailable()) {
            return false;
        }
        if ("_autosave".equals(name)) {
            return loadById("");
        }
        ConfigMetadata profile = catalog.findByName(name, false);
        return profile != null && loadById(profile.id());
    }

    /** Reads and applies a profile without changing the store's selection. */
    public boolean loadById(String id) {
        JsonObject response = io.getProfile(id == null ? "" : id);
        if (!ProfileResponseCodec.isSuccessful(response)) {
            return false;
        }
        catalog.acceptActiveConfigId(ProfileResponseCodec.textField(response, "activeConfigId"));
        String data = ProfileResponseCodec.textField(response, "data");
        return data.isBlank() || loadSnapshot(data);
    }

    /** Captures current state and saves it under a name or the autosave identity. */
    public boolean save(String name) {
        if (clientState.writesDisabled()) {
            return false;
        }
        return catalog.saveSnapshot(name, serialize());
    }

    /** Captures on the caller thread and serializes/saves on the I/O thread. */
    public void saveAsync(String name) {
        if (clientState.writesDisabled()) {
            return;
        }
        JsonObject snapshot = capture();
        io.execute(() -> saveCapturedSnapshot(name, snapshot));
    }

    private void saveCapturedSnapshot(String name, JsonObject snapshot) {
        catalog.saveSnapshot(name, json.toJson(snapshot));
    }

    /** Creates a private profile from current state, capturing before the availability query. */
    public ConfigMetadata create(String name, String description, String type) {
        if (clientState.writesDisabled()) {
            return null;
        }
        String data = serialize();
        return io.isAvailable() ? catalog.save("", name, description, false, type, data) : null;
    }

    /** Updates a private profile, optionally replacing its saved snapshot. */
    public ConfigMetadata updatePrivate(String id, String name, String description, String type, boolean captureState) {
        if (clientState.writesDisabled()) {
            return null;
        }
        String data = captureState ? serialize() : "";
        return io.isAvailable() ? catalog.save(id, name, description, false, type, data) : null;
    }

    /** Updates a public profile, querying availability before optional state capture. */
    public ConfigMetadata updatePublic(String id, String name, String description, String type, boolean captureState) {
        if (clientState.writesDisabled() || !io.isAvailable()) {
            return null;
        }
        return catalog.save(id, name, description, true, type, captureState ? serialize() : "");
    }

    /** Saves private metadata with existing data retained, then applies the returned data. */
    public ConfigMetadata updateMetadataAndApply(String id, String name, String description, String type) {
        if (clientState.writesDisabled() || id == null || id.isBlank() || !io.isAvailable()) {
            return null;
        }
        return applyProfileResponse(io.saveProfile(new ProfileStore.SaveRequest(id, name, description,
                "PRIVATE", type == null ? "GHOST" : type, "")));
    }

    /** Selects a profile and applies the data returned by that selection. */
    public ConfigMetadata selectById(String id) {
        if (!io.isAvailable()) {
            return null;
        }
        return applyProfileResponse(io.selectProfile(id == null ? "" : id));
    }

    /**
     * Performs the original two-selection flow, retaining the first response as fallback.
     *
     * @param selectedId the identifier selected first
     * @param preferredId the identifier selected and applied second
     * @return the preferred metadata, or metadata/data from the first response if it fails
     */
    public ConfigMetadata selectAndApplyPreferred(String selectedId, String preferredId) {
        if (!io.isAvailable()) {
            return null;
        }
        JsonObject first = io.selectProfile(selectedId == null ? "" : selectedId);
        if (!ProfileResponseCodec.isSuccessful(first)) {
            return null;
        }
        ConfigMetadata preferred = selectById(preferredId);
        return preferred != null ? preferred : applyProfileResponse(first);
    }

    /** Deletes a nonblank identifier after checking protocol availability. */
    public boolean delete(String id) {
        return id != null && !id.isBlank() && io.isAvailable() && deleteById(id);
    }

    /** Deletes the preferred private, owned profile matching an untrimmed name. */
    public boolean deleteByName(String name) {
        if (!io.isAvailable()) {
            return false;
        }
        ConfigMetadata profile = catalog.findByName(name, true);
        return profile != null && deleteById(profile.id());
    }

    /** Deletes an identifier and loads a replacement selection if the response supplies one. */
    public boolean deleteById(String id) {
        if (!io.isAvailable()) {
            return false;
        }
        String previous = catalog.activeConfigId();
        JsonObject response = io.deleteProfile(id == null ? "" : id);
        if (!ProfileResponseCodec.isSuccessful(response)) {
            return false;
        }
        catalog.acceptActiveConfigId(ProfileResponseCodec.textField(response, "activeConfigId"));
        if (previous.equals(id) && !catalog.activeConfigId().isBlank()) {
            loadById(catalog.activeConfigId());
        }
        return true;
    }

    /** Applies nonblank returned data even when it fails, then independently reads metadata. */
    private ConfigMetadata applyProfileResponse(JsonObject response) {
        if (!ProfileResponseCodec.isSuccessful(response)) {
            return null;
        }
        catalog.acceptActiveConfigId(ProfileResponseCodec.textField(response, "activeConfigId"));
        String data = ProfileResponseCodec.textField(response, "data");
        if (!data.isBlank()) {
            loadSnapshot(data);
        }
        JsonObject config = ProfileResponseCodec.objectField(response, "config");
        return config == null ? null : ProfileResponseCodec.metadata(config);
    }

    private Feature findFeature(String hash) {
        if (hash == null || clientState.features() == null || clientState.features().getFeatures() == null) {
            return null;
        }
        for (Feature feature : clientState.features().getFeatures()) {
            if (feature.getName() == null) {
                continue;
            }
            if (hash.equals(Concat.shortHash(feature.getName()))) {
                return feature;
            }
        }
        return null;
    }

    private void captureServer(JsonObject snapshot) {
        JsonObject metadata = new JsonObject();
        ServerData server = clientState.server();
        if (server == null) {
            metadata.addProperty("name", "singleplayer");
            metadata.addProperty("ip", "");
            snapshot.add("server", metadata);
            return;
        }
        metadata.addProperty("name", server.name == null ? "" : server.name);
        metadata.addProperty("ip", server.ip == null ? "" : server.ip);
        byte[] icon = server.getIconBytes();
        if (icon != null && icon.length > 0) {
            metadata.addProperty("icon", Base64.getEncoder().encodeToString(icon));
        }
        snapshot.add("server", metadata);
    }

    private void captureFriends(JsonObject snapshot) {
        JsonArray names = new JsonArray();
        List<String> friends = clientState.friends() == null ? List.of() : clientState.friends().names();
        for (String friend : friends) {
            if (friend != null && !friend.isBlank()) {
                names.add(friend);
            }
        }
        snapshot.add("friends", names);
    }

    private void applyFriends(JsonObject snapshot) {
        if (clientState.friends() == null) {
            return;
        }
        List<String> names = new ArrayList<>();
        if (snapshot != null && snapshot.has("friends") && snapshot.get("friends").isJsonArray()) {
            for (JsonElement friend : snapshot.getAsJsonArray("friends")) {
                if (friend != null && !friend.isJsonNull()) {
                    names.add(friend.getAsString());
                }
            }
        }
        clientState.friends().replaceAll(names);
    }

    /** Drains pending profile I/O for at most the original two-second shutdown wait. */
    @Override
    public void close() {
        io.close();
    }

    private static final class LiveClientState implements ClientState {
        @Override public FeatureManager features() { return Echo.featureManager; }
        @Override public FriendManager friends() { return Echo.friendManager; }
        @Override public ServerData server() { return Minecraft.getInstance().getCurrentServer(); }
        @Override public boolean writesDisabled() { return Echo.eventsDisabled; }
        @Override public boolean isClickGui(Feature feature) { return feature instanceof ClickGUIFeature; }
    }
}
