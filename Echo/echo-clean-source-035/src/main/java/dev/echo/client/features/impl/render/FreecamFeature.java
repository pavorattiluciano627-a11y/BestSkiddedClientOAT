package dev.echo.client.features.impl.render;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Event;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.interaction.AttackAttemptEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.network.PacketReceiveEvent;
import dev.echo.client.events.render.OcclusionGraphEvent;
import dev.echo.client.events.render.WorldRenderEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.Setting;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Detached camera state and its original subscribers; see receipt for conditional domains. */
public class FreecamFeature extends Feature {
    private Vec3d cameraPosition;
    private float cameraYaw;
    private float cameraPitch;
    private Perspective previousPerspective;
    private static FloatSetting speed;
    private static boolean perspectiveRefreshBypassed;
    private static final String SPEED_TOOLTIP;

    static {
        MethodHandles.lookup().lookupClass();
        if (isPerspectiveRefreshBypassed()) setPerspectiveRefreshBypassed(true);
        SPEED_TOOLTIP = "Timing";
    }

    public FreecamFeature() {
        super(new FeatureInfo(CatalogText.literal("Freecam"),
                CatalogText.literal("Free movement of camera"), Category.RENDER));
    }

    public static void appendBootstrapSettings(FreecamFeature feature) {
        CatalogSettings.append(feature, speed);
    }

    public static void assignBootstrapSettings(FreecamFeature feature, FloatSetting preparedSpeed) {
        speed = preparedSpeed;
    }

    @Override
    public void postInitialize() {
        Perspective perspective = Perspective.FIRST_PERSON;
        initializeReferencedClass(Perspective.class);
        previousPerspective = perspective;
        initializeReferencedClass(FloatSetting.class);
        FloatSetting speedSetting = speed;
        initializeReferencedClass(Setting.class);
        speedSetting.tooltip(SPEED_TOOLTIP);
    }

    @Override
    public void onEnable() {
        boolean bypass = isPerspectiveRefreshBypassed();
        super.onEnable();
        if (isOutsideWorld()) return;
        initializeReferencedClass(MinecraftClient.class);
        MinecraftClient currentClient = client;
        if (!bypass) {
            if (Objects.requireNonNull(currentClient, "object reference is null").player == null) {
                setEnabled(false);
                return;
            }
            initializeReferencedClass(MinecraftClient.class);
            Vec3d position = Objects.requireNonNull(
                    Objects.requireNonNull(client, "object reference is null").player,
                    "object reference is null").getEyePos();
            initializeReferencedClass(Vec3d.class);
            cameraPosition = position;
            initializeReferencedClass(MinecraftClient.class);
            currentClient = client;
        }
        Perspective perspective = Objects.requireNonNull(
                Objects.requireNonNull(currentClient, "object reference is null").options,
                "object reference is null").getPerspective();
        if (!bypass && perspective == null) perspective = Perspective.FIRST_PERSON;
        initializeReferencedClass(Perspective.class);
        previousPerspective = perspective;
        initializeReferencedClass(MinecraftClient.class);
        cameraYaw = Objects.requireNonNull(
                Objects.requireNonNull(client, "object reference is null").player,
                "object reference is null").getYaw();
        initializeReferencedClass(MinecraftClient.class);
        cameraPitch = Objects.requireNonNull(
                Objects.requireNonNull(client, "object reference is null").player,
                "object reference is null").getPitch();
    }

    /** NB-083: both state branches retained; structural checking is not unconditional admission. */
    @Override
    public void onDisable() {
        boolean useStoredPerspective = isPerspectiveRefreshBypassed();
        super.onDisable();
        initializeReferencedClass(Vec3d.class);
        cameraPosition = null;
        initializeReferencedClass(MinecraftClient.class);
        MinecraftClient currentClient = client;
        if (!useStoredPerspective) {
            if (currentClient == null) return;
            initializeReferencedClass(MinecraftClient.class);
            currentClient = client;
        }
        if (Objects.requireNonNull(currentClient, "object reference is null").options == null) return;
        initializeReferencedClass(Perspective.class);
        Perspective perspective = previousPerspective;
        if (!useStoredPerspective) {
            if (perspective == null) return;
            initializeReferencedClass(MinecraftClient.class);
            MinecraftClient perspectiveClient = client;
            perspective = Objects.requireNonNull(
                    Objects.requireNonNull(perspectiveClient, "object reference is null").options,
                    "object reference is null").getPerspective();
        }
        initializeReferencedClass(Perspective.class);
        if (perspective == previousPerspective) return;
        try {
            initializeReferencedClass(MinecraftClient.class);
            MinecraftClient restoreClient = client;
            GameOptions options = Objects.requireNonNull(restoreClient, "object reference is null").options;
            initializeReferencedClass(Perspective.class);
            Perspective restore = previousPerspective;
            Objects.requireNonNull(options, "object reference is null").setPerspective(restore);
        } catch (Exception ignored) {
            // Only this final restoration region suppresses exceptions in the original.
        }
    }

    public void rotateCamera(float yawDelta, float pitchDelta) {
        cameraYaw += yawDelta * 0.15F;
        cameraPitch = MathHelper.clamp(cameraPitch + pitchDelta * 0.15F, -90.0F, 90.0F);
    }

    @Subscribe
    private void onOcclusionGraph(OcclusionGraphEvent event) {
        initializeReferencedClass(Event.class);
        event.cancel();
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        boolean bypass = isPerspectiveRefreshBypassed();
        if (!bypass && isOutsideWorld()) return;
        initializeReferencedClass(Vec3d.class);
        if (cameraPosition == null) return;
        initializeReferencedClass(FloatSetting.class);
        FloatSetting speedSetting = speed;
        initializeReferencedClass(FloatSetting.class);
        float speedValue = speedSetting.getValue();
        initializeReferencedClass(WorldRenderEvent.class);
        float distance = speedValue * event.getDynamicDeltaTicks();
        Vec3d forward = Vec3d.fromPolar(cameraPitch, cameraYaw);
        Vec3d right = forward.crossProduct(new Vec3d(0.0, 1.0, 0.0)).normalize();
        initializeReferencedClass(MinecraftClient.class);
        KeyBinding backward = client.options.forwardKey;
        if (!bypass) {
            if (backward.isPressed()) {
                initializeReferencedClass(Vec3d.class);
                Vec3d position = cameraPosition.add(forward.multiply(distance));
                initializeReferencedClass(Vec3d.class);
                cameraPosition = position;
            }
            initializeReferencedClass(MinecraftClient.class);
            backward = client.options.backKey;
        }
        if (backward.isPressed()) {
            initializeReferencedClass(Vec3d.class);
            Vec3d position = cameraPosition.subtract(forward.multiply(distance));
            initializeReferencedClass(Vec3d.class);
            cameraPosition = position;
        }
        initializeReferencedClass(MinecraftClient.class);
        MinecraftClient lateralClient = client;
        if (!bypass) {
            if (lateralClient.options.leftKey.isPressed()) {
                initializeReferencedClass(Vec3d.class);
                Vec3d position = cameraPosition.subtract(right.multiply(distance));
                initializeReferencedClass(Vec3d.class);
                cameraPosition = position;
            }
            initializeReferencedClass(MinecraftClient.class);
            lateralClient = client;
        }
        KeyBinding upward = lateralClient.options.rightKey;
        if (!bypass) {
            if (upward.isPressed()) {
                initializeReferencedClass(Vec3d.class);
                Vec3d position = cameraPosition.add(right.multiply(distance));
                initializeReferencedClass(Vec3d.class);
                cameraPosition = position;
            }
            initializeReferencedClass(MinecraftClient.class);
            upward = client.options.jumpKey;
        }
        if (upward.isPressed()) {
            initializeReferencedClass(Vec3d.class);
            Vec3d position = cameraPosition.add(0.0, distance, 0.0);
            initializeReferencedClass(Vec3d.class);
            cameraPosition = position;
        }
        initializeReferencedClass(MinecraftClient.class);
        if (client.options.sneakKey.isPressed()) {
            initializeReferencedClass(Vec3d.class);
            Vec3d position = cameraPosition.subtract(0.0, distance, 0.0);
            initializeReferencedClass(Vec3d.class);
            cameraPosition = position;
        }
    }

    @Subscribe
    private void onAttackAttempt(AttackAttemptEvent event) {
        initializeReferencedClass(Event.class);
        event.cancel();
    }

    @Subscribe
    private void onPacketReceive(PacketReceiveEvent event) {
        boolean checkPacketType = isPerspectiveRefreshEnabled();
        initializeReferencedClass(PacketReceiveEvent.class);
        Packet<?> packet = event.getPacket();
        if (checkPacketType && !(packet instanceof DeathMessageS2CPacket)) return;
        DeathMessageS2CPacket death = (DeathMessageS2CPacket) packet;
        initializeReferencedClass(MinecraftClient.class);
        if (client.player == null) return;
        int playerId = death.playerId();
        initializeReferencedClass(MinecraftClient.class);
        if (playerId == client.player.getId()) setEnabled(false);
    }

    @Subscribe
    private void onClientTick(ClientTickEvent event) {
        boolean bypass = isPerspectiveRefreshBypassed();
        initializeReferencedClass(MinecraftClient.class);
        MinecraftClient currentClient = client;
        if (!bypass) {
            if (currentClient.options.getPerspective() == Perspective.FIRST_PERSON) return;
            initializeReferencedClass(MinecraftClient.class);
            currentClient = client;
        }
        currentClient.options.setPerspective(Perspective.FIRST_PERSON);
    }

    public Vec3d getCameraPosition() {
        initializeReferencedClass(Vec3d.class);
        return cameraPosition;
    }

    public float getCameraYaw() { return cameraYaw; }
    public float getCameraPitch() { return cameraPitch; }

    public static void setPerspectiveRefreshBypassed(boolean bypassed) {
        perspectiveRefreshBypassed = bypassed;
    }

    public static boolean isPerspectiveRefreshBypassed() {
        return perspectiveRefreshBypassed;
    }

    public static boolean isPerspectiveRefreshEnabled() {
        return !isPerspectiveRefreshBypassed();
    }

    /** Type initialization only, preserving the original descriptor-resolution side effects. */
    private static void initializeReferencedClass(Class<?> type) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }
}
