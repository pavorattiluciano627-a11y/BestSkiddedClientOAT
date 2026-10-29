package dev.echo.client.bootstrap;

import dev.echo.client.Echo;
import dev.echo.client.chunk.BlockEspManager;
import dev.echo.client.chunk.ChunkManager;
import dev.echo.client.config.FeatureConfig;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureManager;
import dev.echo.client.friends.FriendManager;
import dev.echo.client.handlers.HandlerManager;
import dev.echo.client.integration.OfflineRuntimeSession;
import dev.echo.client.particle.ParticleManager;
import dev.echo.client.screens.ScreenManager;
import dev.echo.client.spotify.SpotifyManager;
import dev.echo.client.utils.audio.EchoAudio;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/** Executes the recovered service/allocation/setting/registration startup order. */
public final class ClientBootstrap {
    private static final int FEATURE_DEFINITION_COUNT = 98;
    /** Once-only startup state; failed initialization retains its original cause. */
    public enum State { NEW, PREPARED, INITIALIZING, INITIALIZED, FAILED }

    /** A constructor failure retained alongside the actual successful allocations. */
    public record SkippedFeature(String name, Throwable cause) {
    }

    /** The resulting source-owned feature instances and category registrations. */
    public record Result(Echo echo, List<Feature> allocated, List<Feature> registered,
                         Map<Category, List<Feature>> categories, List<SkippedFeature> skipped) {
    }

    private static State state = State.NEW;
    private static Result result;
    private static Throwable failure;

    private ClientBootstrap() {
    }

    /** Establishes the shared offline session before any client service construction. */
    public static synchronized void prepare() {
        if (state == State.PREPARED || state == State.INITIALIZED) {
            return;
        }
        if (state != State.NEW) {
            throw new IllegalStateException("Cannot preLaunch from " + state, failure);
        }
        OfflineRuntimeSession.shared().markInitialized();
        state = State.PREPARED;
    }

    /**
     * Runs the real source catalog at Fabric's main-initializer boundary.
     * The guard precedes catalog evaluation, including its class initialization.
     *
     * @return the once-only source bootstrap result
     */
    public static synchronized Result initializeCatalog() {
        if (state == State.INITIALIZED) {
            return result;
        }
        requirePrepared();
        return initializeGraph(FeatureCatalog::definitions);
    }

    /**
     * Initializes the concrete feature catalog and client services exactly once.
     * The catalog owns the complete feature and setting definitions, including
     * their per-feature construction and assignment order.
     *
     * @param definitions the source catalog in its supplied allocation order;
     *                    equivalence to the native map order is a separate contract
     * @return the actual allocation and registration result
     */
    public static synchronized Result initialize(List<FeatureDefinition> definitions) {
        if (state == State.INITIALIZED) {
            return result;
        }
        requirePrepared();
        if (definitions.size() != FEATURE_DEFINITION_COUNT) {
            throw new IllegalArgumentException("Expected 98 source feature definitions");
        }
        return initializeGraph(() -> definitions);
    }

    private static void requirePrepared() {
        if (state != State.PREPARED) {
            throw new IllegalStateException("Expected preLaunch, state=" + state, failure);
        }
    }

    /** Called only while holding the class monitor, after the public state checks. */
    private static Result initializeGraph(Supplier<List<FeatureDefinition>> definitionSource) {
        state = State.INITIALIZING;
        try {
            Echo.friendManager = new FriendManager();
            Echo.particleManager = new ParticleManager();
            Echo.handlerManager = new HandlerManager();
            Echo.chunkManager = new ChunkManager();
            EchoAudio.preload();

            Echo.featureManager = new FeatureManager();
            Echo.featureConfig = new FeatureConfig();
            Echo.featureConfig.loading = true;

            // The catalog contains enum references and typed constructor handles.
            // Resolve it here, at the original feature-initialization boundary,
            // rather than during preLaunch or before service construction.
            List<FeatureDefinition> definitions = definitionSource.get();
            if (definitions.size() != FEATURE_DEFINITION_COUNT) {
                throw new IllegalArgumentException("Expected 98 source feature definitions");
            }
            List<Feature> allocated = new ArrayList<>();
            List<FeatureDefinition> allocatedDefinitions = new ArrayList<>();
            List<SkippedFeature> skipped = new ArrayList<>();
            for (FeatureDefinition definition : definitions) {
                Feature feature;
                try {
                    feature = definition.constructor().get();
                    if (feature == null) {
                        throw new IllegalStateException("Feature constructor returned null: " + definition.name());
                    }
                } catch (Throwable exception) {
                    skipped.add(new SkippedFeature(definition.name(), exception));
                    Echo.LOGGER.error("Skipped feature allocation " + definition.name(), exception);
                    continue;
                }
                allocated.add(feature);
                allocatedDefinitions.add(definition);
            }

            CopyOnWriteArrayList<Feature> featureList = new CopyOnWriteArrayList<>();
            Map<Feature, Category> featureCategories = new IdentityHashMap<>();
            for (int index = 0; index < allocated.size(); index++) {
                Feature feature = allocated.get(index);
                FeatureDefinition definition = allocatedDefinitions.get(index);
                definition.initializeSettings().accept(feature);
                featureList.add(feature);
                featureCategories.put(feature, definition.category());
            }

            CopyOnWriteArrayList<Feature> sorted;
            try {
                sorted = Echo.featureManager.sort(featureList);
            } catch (Throwable exception) {
                Echo.LOGGER.error("FeatureManager.sort threw; native fallback preserves the input list", exception);
                sorted = featureList;
            }
            if (sorted == null) {
                throw new IllegalStateException("FeatureManager.sort returned no list");
            }
            Echo.featureManager.addFeatures(sorted);

            Map<Category, List<Feature>> categories = new ConcurrentHashMap<>();
            Category[] categoryValues = Category.values();
            for (Category category : categoryValues) {
                categories.put(category, new ArrayList<>());
            }
            for (Feature feature : sorted) {
                Category category = featureCategories.getOrDefault(feature, Category.INTERNALS);
                categories.get(category).add(feature);
            }
            for (Category category : categoryValues) {
                categories.put(category, Collections.unmodifiableList(categories.get(category)));
            }
            Echo.featureManager.addCategoryCache(categories);

            Echo.screenManager = new ScreenManager();
            Echo.screenManager.initialize();
            Echo.blockEspManager = new BlockEspManager();
            Echo.spotifyManager = new SpotifyManager();
            Echo.handlerManager.initialize();
            Echo echo = new Echo();
            echo.onInitialize();
            Echo.LOGGER.info("[echo] bootstrap INITIALIZED: features={} registered={} "
                    + "skipped={} handlers={} screenManager={}",
                    allocated.size(), sorted.size(), skipped.size(),
                    Echo.handlerManager != null ? Echo.handlerManager.size() : -1,
                    Echo.screenManager != null);
            for (SkippedFeature miss : skipped) {
                Echo.LOGGER.error("[echo] feature not allocated: {} -> {}",
                        miss.name(), miss.cause() == null ? "?" : miss.cause().toString());
            }

            result = new Result(echo, List.copyOf(allocated), List.copyOf(sorted),
                    Collections.unmodifiableMap(categories), List.copyOf(skipped));
            state = State.INITIALIZED;
            return result;
        } catch (RuntimeException | Error exception) {
            state = State.FAILED;
            failure = exception;
            throw exception;
        }
    }

    /** Returns the current once-only lifecycle state. */
    public static synchronized State state() {
        return state;
    }
}
