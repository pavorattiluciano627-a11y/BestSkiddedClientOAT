package dev.echo.client.features.impl.render;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.render.WorldRenderEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.render.DepthTestedWorldGeometry;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.utils.Rotation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Draws server and visible attack-ray endpoints using the held item's actual attack range. */
public class AimVisualizerFeature extends Feature {
    private static FloatSetting size;
    private static ColorSetting serverColor;
    private static ColorSetting clientColor;

    public AimVisualizerFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Aim Visualizer"), dev.echo.client.bootstrap.CatalogText.literal("Shows where server-side rotations are aiming"), dev.echo.client.features.Category.RENDER, false));
    }

    @Override
    public void postInitialize() {
        size.tooltip("Appearance");
        serverColor.tooltip("Appearance");
        clientColor.tooltip("Appearance");
    }

    private Vec3 findEndpoint(Vec3 origin, float pitch, float yaw, AttackRange range) {
        Vec3 direction = client.player.calculateViewVector(pitch, yaw);
        float hitboxMargin = range.hitboxMargin();
        Vec3 minimum = origin.add(direction.scale(range.effectiveMinRange(client.player)));
        Vec3 maximum = origin.add(direction.scale(range.effectiveMaxRange(client.player)
                + Math.max(0.0, client.player.getKnownMovement().dot(direction))));
        BlockHitResult blockHit = client.level.clip(new ClipContext(origin, maximum,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, client.player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            maximum = blockHit.getLocation();
            if (origin.distanceToSqr(maximum) < origin.distanceToSqr(minimum)) {
                return maximum;
            }
        }
        AABB search = AABB.ofSize(minimum, hitboxMargin, hitboxMargin, hitboxMargin)
                .expandTowards(maximum.subtract(minimum)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(client.level, client.player,
                minimum, maximum, search, EntitySelector.CAN_BE_PICKED, hitboxMargin);
        return entityHit != null ? entityHit.getLocation() : maximum;
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        if (client.player == null || client.level == null) {
            return;
        }
        float tickProgress = event.getTickProgress();
        Vec3 origin = client.player.getEyePosition(tickProgress);
        ItemStack heldStack = client.player.getMainHandItem();
        AttackRange range = heldStack.get(DataComponents.ATTACK_RANGE);
        if (range == null) {
            range = AttackRange.defaultFor(client.player);
        }
        boolean showServerAim = rotations().isSilentRotationActive();
        Vec3 serverEndpoint = null;
        if (showServerAim) {
            float[] angles = getServerAngles();
            float yaw = angles[0];
            float pitch = angles[1];
            serverEndpoint = findEndpoint(origin, pitch, yaw, range);
        }
        float clientYaw = client.player.getYRot(tickProgress);
        float clientPitch = client.player.getXRot(tickProgress);
        Vec3 clientEndpoint = findEndpoint(origin, clientPitch, clientYaw, range);
        int serverArgb = ARGB.colorFromFloat(serverColor.getAlphaFraction(),
                serverColor.getRedFraction(), serverColor.getGreenFraction(), serverColor.getBlueFraction());
        int clientArgb = ARGB.colorFromFloat(clientColor.getAlphaFraction(),
                clientColor.getRedFraction(), clientColor.getGreenFraction(), clientColor.getBlueFraction());
        float serverSize = size.getValue();
        double clientSize = (double) serverSize - 0.001;
        DepthTestedWorldGeometry geometry = event.getRenderContext().getDepthTestedGeometry();
        if (showServerAim) {
            geometry.drawBox(serverEndpoint.x - (double) serverSize * 0.5,
                    serverEndpoint.y - (double) serverSize * 0.5,
                    serverEndpoint.z - (double) serverSize * 0.5,
                    serverSize, serverSize, serverSize, serverArgb);
        }
        geometry.drawBox(clientEndpoint.x - clientSize * 0.5,
                clientEndpoint.y - clientSize * 0.5, clientEndpoint.z - clientSize * 0.5,
                clientSize, clientSize, clientSize, clientArgb);
    }

    private float[] getServerAngles() {
        if (rotations().isSilentRotationActive()) {
            Rotation rotation = rotations().effectiveRotation();
            return new float[] {rotation.yaw(), rotation.pitch()};
        }
        Rotation rotation = rotations().effectiveRotation();
        return new float[] {rotation.yaw(), rotation.pitch()};
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(AimVisualizerFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AimVisualizerFeature.size);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AimVisualizerFeature.serverColor);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AimVisualizerFeature.clientColor);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(AimVisualizerFeature feature, dev.echo.client.setting.FloatSetting preparedSize, dev.echo.client.setting.ColorSetting preparedServerColor, dev.echo.client.setting.ColorSetting preparedClientColor) {
        AimVisualizerFeature.size = preparedSize;
        AimVisualizerFeature.serverColor = preparedServerColor;
        AimVisualizerFeature.clientColor = preparedClientColor;
    }
}
