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
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Detached camera state and its original subscribers; see receipt for conditional domains. */
public class FreecamFeature extends Feature {
    private Vec3 cameraPosition;
    private float cameraYaw;
    private float cameraPitch;
    private CameraType previousPerspective;
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
        CameraType perspective = CameraType.FIRST_PERSON;
        initializeReferencedClass(CameraType.class);
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
        initializeReferencedClass(Minecraft.class);
        Minecraft currentClient = client;
        if (!bypass) {
            if (Objects.requireNonNull(currentClient, "object reference is null").player == null) {
                setEnabled(false);
                return;
            }
            initializeReferencedClass(Minecraft.class);
            Vec3 position = Objects.requireNonNull(
                    Objects.requireNonNull(client, "object reference is null").player,
                    "object reference is null").getEyePosition();
            initializeReferencedClass(Vec3.class);
            cameraPosition = position;
            initializeReferencedClass(Minecraft.class);
            currentClient = client;
        }
        CameraType perspective = Objects.requireNonNull(
                Objects.requireNonNull(currentClient, "object reference is null").options,
                "object reference is null").getCameraType();
        if (!bypass && perspective == null) perspective = CameraType.FIRST_PERSON;
        initializeReferencedClass(CameraType.class);
        previousPerspective = perspective;
        initializeReferencedClass(Minecraft.class);
        cameraYaw = Objects.requireNonNull(
                Objects.requireNonNull(client, "object reference is null").player,
                "object reference is null").getYRot();
        initializeReferencedClass(Minecraft.class);
        cameraPitch = Objects.requireNonNull(
                Objects.requireNonNull(client, "object reference is null").player,
                "object reference is null").getXRot();
    }

    /** NB-083: both state branches retained; structural checking is not unconditional admission. */
    @Override
    public void onDisable() {
        boolean useStoredPerspective = isPerspectiveRefreshBypassed();
        super.onDisable();
        initializeReferencedClass(Vec3.class);
        cameraPosition = null;
        initializeReferencedClass(Minecraft.class);
        Minecraft currentClient = client;
        if (!useStoredPerspective) {
            if (currentClient == null) return;
            initializeReferencedClass(Minecraft.class);
            currentClient = client;
        }
        if (Objects.requireNonNull(currentClient, "object reference is null").options == null) return;
        initializeReferencedClass(CameraType.class);
        CameraType perspective = previousPerspective;
        if (!useStoredPerspective) {
            if (perspective == null) return;
            initializeReferencedClass(Minecraft.class);
            Minecraft perspectiveClient = client;
            perspective = Objects.requireNonNull(
                    Objects.requireNonNull(perspectiveClient, "object reference is null").options,
                    "object reference is null").getCameraType();
        }
        initializeReferencedClass(CameraType.class);
        if (perspective == previousPerspective) return;
        try {
            initializeReferencedClass(Minecraft.class);
            Minecraft restoreClient = client;
            Options options = Objects.requireNonNull(restoreClient, "object reference is null").options;
            initializeReferencedClass(CameraType.class);
            CameraType restore = previousPerspective;
            Objects.requireNonNull(options, "object reference is null").setCameraType(restore);
        } catch (Exception ignored) {
            // Only this final restoration region suppresses exceptions in the original.
        }
    }

    public void rotateCamera(float yawDelta, float pitchDelta) {
        cameraYaw += yawDelta * 0.15F;
        cameraPitch = Mth.clamp(cameraPitch + pitchDelta * 0.15F, -90.0F, 90.0F);
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
        initializeReferencedClass(Vec3.class);
        if (cameraPosition == null) return;
        initializeReferencedClass(FloatSetting.class);
        FloatSetting speedSetting = speed;
        initializeReferencedClass(FloatSetting.class);
        float speedValue = speedSetting.getValue();
        initializeReferencedClass(WorldRenderEvent.class);
        float distance = speedValue * event.getDynamicDeltaTicks();
        Vec3 forward = Vec3.directionFromRotation(cameraPitch, cameraYaw);
        Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
        initializeReferencedClass(Minecraft.class);
        KeyMapping backward = client.options.keyUp;
        if (!bypass) {
            if (backward.isDown()) {
                initializeReferencedClass(Vec3.class);
                Vec3 position = cameraPosition.add(forward.scale(distance));
                initializeReferencedClass(Vec3.class);
                cameraPosition = position;
            }
            initializeReferencedClass(Minecraft.class);
            backward = client.options.keyDown;
        }
        if (backward.isDown()) {
            initializeReferencedClass(Vec3.class);
            Vec3 position = cameraPosition.subtract(forward.scale(distance));
            initializeReferencedClass(Vec3.class);
            cameraPosition = position;
        }
        initializeReferencedClass(Minecraft.class);
        Minecraft lateralClient = client;
        if (!bypass) {
            if (lateralClient.options.keyLeft.isDown()) {
                initializeReferencedClass(Vec3.class);
                Vec3 position = cameraPosition.subtract(right.scale(distance));
                initializeReferencedClass(Vec3.class);
                cameraPosition = position;
            }
            initializeReferencedClass(Minecraft.class);
            lateralClient = client;
        }
        KeyMapping upward = lateralClient.options.keyRight;
        if (!bypass) {
            if (upward.isDown()) {
                initializeReferencedClass(Vec3.class);
                Vec3 position = cameraPosition.add(right.scale(distance));
                initializeReferencedClass(Vec3.class);
                cameraPosition = position;
            }
            initializeReferencedClass(Minecraft.class);
            upward = client.options.keyJump;
        }
        if (upward.isDown()) {
            initializeReferencedClass(Vec3.class);
            Vec3 position = cameraPosition.add(0.0, distance, 0.0);
            initializeReferencedClass(Vec3.class);
            cameraPosition = position;
        }
        initializeReferencedClass(Minecraft.class);
        if (client.options.keyShift.isDown()) {
            initializeReferencedClass(Vec3.class);
            Vec3 position = cameraPosition.subtract(0.0, distance, 0.0);
            initializeReferencedClass(Vec3.class);
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
        if (checkPacketType && !(packet instanceof ClientboundPlayerCombatKillPacket)) return;
        ClientboundPlayerCombatKillPacket death = (ClientboundPlayerCombatKillPacket) packet;
        initializeReferencedClass(Minecraft.class);
        if (client.player == null) return;
        int playerId = death.playerId();
        initializeReferencedClass(Minecraft.class);
        if (playerId == client.player.getId()) setEnabled(false);
    }

    @Subscribe
    private void onClientTick(ClientTickEvent event) {
        boolean bypass = isPerspectiveRefreshBypassed();
        initializeReferencedClass(Minecraft.class);
        Minecraft currentClient = client;
        if (!bypass) {
            if (currentClient.options.getCameraType() == CameraType.FIRST_PERSON) return;
            initializeReferencedClass(Minecraft.class);
            currentClient = client;
        }
        currentClient.options.setCameraType(CameraType.FIRST_PERSON);
    }

    public Vec3 getCameraPosition() {
        initializeReferencedClass(Vec3.class);
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
