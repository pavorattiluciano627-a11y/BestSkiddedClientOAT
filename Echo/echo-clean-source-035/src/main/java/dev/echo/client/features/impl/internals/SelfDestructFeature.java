package dev.echo.client.features.impl.internals;

import dev.echo.client.Echo;
import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.config.FeatureConfig;
import dev.echo.client.config.FeatureNameAssignments;
import dev.echo.client.events.EventBus;
import dev.echo.client.feature.internals.ClientCleanupAction;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.hud.HudRenderer;
import dev.echo.render.FullscreenEffects;
import dev.echo.client.screens.ScreenManager;
import dev.echo.client.screens.dashboard.DashboardController;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.TextSetting;
import dev.echo.client.utils.audio.EchoAudio;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

/** Deferred terminal cleanup candidate; provider, key/cache and initialization review remains open. */
public class SelfDestructFeature extends Feature {
    private boolean destructionStarted;
    private static BooleanSetting replaceMods;
    private static TextSetting url;

    public SelfDestructFeature() {
        super(new FeatureInfo(CatalogText.literal("Self Destruct"),
                CatalogText.literal("Self destructs the client to hide any traces of itself"), Category.INTERNALS));
    }

    @Override
    public void postInitialize() {
        url.setVisibilityPredicate(ignored -> replaceMods.getValue());
    }

    @Override
    public boolean isPersistent() { return false; }

    @Override
    public void onEnable() {
        if (destructionStarted || Echo.eventsDisabled) return;
        initializeClass(FeatureConfig.class);
        if (Echo.featureConfig != null) {
            try {
                initializeClass(FeatureConfig.class);
                Echo.featureConfig.save("_autosave");
            } catch (RuntimeException saveFailure) {
                // Autosave alone is best-effort; no later teardown failure is absorbed here.
            }
        }
        destructionStarted = true;
        Echo.eventsDisabled = true;
        initializeClass(ScreenManager.class);
        if (Echo.screenManager != null) {
            initializeClass(ScreenManager.class);
            Echo.screenManager.close();
        }
        initializeClass(MinecraftClient.class);
        client.send(this::finishSelfDestruct);
    }

    private void finishSelfDestruct() {
        List<ClientCleanupAction> actions = new ClientStringCleanup(Echo.class).collectActions(
                FeatureNameAssignments.class, () -> DashboardController.INSTANCE, () -> HudRenderer.INSTANCE);
        if (replaceMods.getValue()) replaceModFiles();
        if (Echo.screenManager != null) {
            Echo.screenManager.close();
            Echo.screenManager.destroy();
        }
        if (Echo.handlerManager != null) Echo.handlerManager.shutdown();
        if (Echo.featureManager != null) Echo.featureManager.shutdownExcept(this);
        EchoAudio.shutdown();
        FullscreenEffects.releaseResources();
        EventBus.clear();
        Echo.externalRendererActive = false;
        if (Echo.externalRenderer != null) {
            try {
                Echo.externalRenderer.close();
            } catch (RuntimeException rendererFailure) {
                // This release failure is tolerated; publication below still occurs.
            }
            Echo.externalRenderer = null;
        }
        Echo.screenManager = null;
        Echo.handlerManager = null;
        Echo.featureManager = null;
        setEnabled(false);
        if (Echo.featureConfig != null) Echo.featureConfig.close();
        Echo.featureConfig = null;
        Set<String> strings = Collections.newSetFromMap(new IdentityHashMap<>());
        FeatureNameAssignments.collectAssignedNames(strings);
        HudRenderer.INSTANCE.collectKeybindIds(strings);
        HudRenderer.INSTANCE.resetTransientState();
        FeatureNameAssignments.clearAssignments();
        actions.forEach(ClientCleanupAction::clear);
        actions.clear();
        ClientStringCleanup.clearStrings(strings);
        strings.clear();
        System.gc();
    }

    /** The original createWindow(J)V is exactly RETURN; the settings have no replacement effect. */
    private void replaceModFiles() {
        return;
    }

    /**
     * Resolves the artifact origin using explicit decoded protocol/mod-ID data.
     * This private helper has no caller in this artifact's replacement callback.
     */
    private Path resolveModPath(String expectedProtocol, String modId) {
        try {
            CodeSource source = Echo.class.getProtectionDomain().getCodeSource();
            if (source != null && source.getLocation() != null
                    && expectedProtocol.equalsIgnoreCase(source.getLocation().getProtocol())) {
                Path path = Path.of(source.getLocation().toURI());
                if (Files.isRegularFile(path)) return path;
            }
        } catch (Exception unavailableCodeSource) {
            // The fallback is outside this catch, so Fabric/path-filter failures propagate.
        }
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getOrigin().getPaths())
                .filter(paths -> !paths.isEmpty())
                .map(paths -> paths.get(0))
                .filter(Files::isRegularFile)
                .orElse(null);
    }

    private static void initializeClass(Class<?> type) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(SelfDestructFeature feature) {
        CatalogSettings.append(feature, replaceMods);
        CatalogSettings.append(feature, url);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(SelfDestructFeature feature,
            BooleanSetting preparedReplaceMods, TextSetting preparedUrl) {
        replaceMods = preparedReplaceMods;
        url = preparedUrl;
    }
}
