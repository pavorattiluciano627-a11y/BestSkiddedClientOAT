package dev.echo.client;

import dev.echo.client.chunk.BlockEspManager;
import dev.echo.client.chunk.ChunkManager;
import dev.echo.client.config.FeatureConfig;
import dev.echo.client.events.EventBus;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.lifecycle.FrameStartEvent;
import dev.echo.client.features.FeatureManager;
import dev.echo.client.friends.FriendManager;
import dev.echo.client.handlers.HandlerManager;
import dev.echo.client.hud.HudRenderer;
import dev.echo.client.integration.SystemInformation;
import dev.echo.client.particle.ParticleManager;
import dev.echo.client.render.GlFramebufferBridge;
import dev.echo.client.render.RenderBackendManager;
import dev.echo.client.render.RendererPlatformBridge;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.render.WorldRenderContext;
import dev.echo.client.screens.ScreenManager;
import dev.echo.client.screens.dashboard.DashboardController;
import dev.echo.client.scripting.ScriptEngine;
import dev.echo.client.scripting.ScriptManager;
import dev.echo.client.spotify.SpotifyManager;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.opengl.OpenGlDevice;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client services and the original resource-driven renderer lifecycle. */
public class Echo implements ModInitializer, SimpleSynchronousResourceReloadListener {
    public static final String ID;
    public static Echo instance;
    public static FeatureManager featureManager;
    public static FriendManager friendManager;
    public static ScreenManager screenManager;
    public static FeatureConfig featureConfig;
    public static HandlerManager handlerManager;
    public static BlockEspManager blockEspManager;

    /** Mutable cross-thread lifecycle gate read by event registration and dispatch. */
    public static volatile boolean eventsDisabled;

    public static long frameNumber;
    public static WorldRenderContext worldRenderer;
    public static ChunkManager chunkManager;
    public static SpotifyManager spotifyManager;
    public static ParticleManager particleManager;
    public static RenderDevice renderDevice;
    public static CommandEncoder commandEncoder;
    public static UiRenderer uiRenderer;
    public static RenderBackendManager externalRenderer;
    public static boolean externalRendererActive;
    public static ScriptEngine scriptEngine;
    public static ScriptManager scriptManager;
    public static final Logger LOGGER;

    static {
        ID = "echo";
        eventsDisabled = false;
        externalRendererActive = false;
        LOGGER = LoggerFactory.getLogger("echo");
    }

    /** Registers the actual client and dashboard after the service graph is constructed. */
    @Override
    public void onInitialize() {
        instance = this;
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(this);
        EventBus.register(this);
        EventBus.register(DashboardController.INSTANCE);
    }

    /** Starts a device frame and publishes its current command encoder. */
    @Subscribe
    public void onFrameStart(FrameStartEvent event) {
        initializeReferencedClass(RenderDevice.class);
        if (renderDevice != null) {
            frameNumber++;
            renderDevice.beginFrame();
            commandEncoder = renderDevice.commandEncoder();
        }
    }

    /**
     * Creates the default renderer with the original platform/capability order.
     * @return the configured OpenGL device
     */
    private static RenderDevice createDefaultRenderDevice() {
        RendererPlatformBridge platform = new RendererPlatformBridge();
        return new OpenGlDevice(new GlFramebufferBridge(), platform, OpenGlDevice.queryCapabilities(false));
    }

    /** Initializes renderer consumers and then completes the deferred autosave load. */
    @Override
    public void reload(ResourceManager resources) {
        initializeReferencedClass(UiRenderer.class);
        if (uiRenderer != null) {
            return;
        }
        initializeReferencedClass(RenderDevice.class);
        if (renderDevice == null) {
            renderDevice = createDefaultRenderDevice();
            uiRenderer = new UiRenderer(renderDevice);
            worldRenderer = new WorldRenderContext(renderDevice);
            DashboardController.INSTANCE.initialize(renderDevice);
            HudRenderer.INSTANCE.initialize(renderDevice);
            EventBus.register(HudRenderer.INSTANCE);
        }
        initializeReferencedClass(ParticleManager.class);
        if (particleManager == null) {
            return;
        }
        initializeReferencedClass(FeatureManager.class);
        if (featureManager == null) {
            return;
        }
        initializeReferencedClass(FeatureConfig.class);
        if (featureConfig == null || !featureConfig.loading) {
            return;
        }
        particleManager.initialize();
        featureConfig.load("_autosave");
        featureConfig.loading = false;
        SystemInformation.send();
    }

    /** Returns the resource-reload listener's existing compatibility identifier. */
    @Override
    public Identifier getFabricId() {
        return Identifier.of("echo", "echo");
    }

    /**
     * Enables or releases the external render backend and its owned window/resources.
     * Construction failures are logged; cleanup failures propagate before the final
     * active-state update. The event-dispatch lifecycle gate is independent.
     *
     * @param enabled whether the external renderer is requested
     */
    public static void setExternalRendererEnabled(boolean enabled) {
        initializeReferencedClass(RenderBackendManager.class);
        if (enabled) {
            if (externalRenderer == null) {
                try {
                    externalRenderer = new RenderBackendManager();
                } catch (Throwable failure) {
                    LOGGER.error("Stream proof overlay unavailable; stream proof disabled", failure);
                }
            }
        } else if (externalRenderer != null) {
            externalRenderer.close();
            externalRenderer = null;
            initializeReferencedClass(UiRenderer.class);
            uiRenderer.clearFrameQueues();
        }
        externalRendererActive = enabled && externalRenderer != null;
    }

    /** Preserves the original resolved type's initialization before a nullable field read. */
    private static void initializeReferencedClass(Class<?> type) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }
}
