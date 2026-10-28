package dev.echo.client.features.impl.render;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.render.WorldRenderEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.render.DepthTestedWorldGeometry;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.utils.Rotation;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

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

    private Vec3d findEndpoint(Vec3d origin, float pitch, float yaw, AttackRangeComponent range) {
        Vec3d direction = client.player.getRotationVector(pitch, yaw);
        float hitboxMargin = range.hitboxMargin();
        Vec3d minimum = origin.add(direction.multiply(range.getEffectiveMinRange(client.player)));
        Vec3d maximum = origin.add(direction.multiply(range.getEffectiveMaxRange(client.player)
                + Math.max(0.0, client.player.getMovement().dotProduct(direction))));
        BlockHitResult blockHit = client.world.raycast(new RaycastContext(origin, maximum,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            maximum = blockHit.getPos();
            if (origin.squaredDistanceTo(maximum) < origin.squaredDistanceTo(minimum)) {
                return maximum;
            }
        }
        Box search = Box.of(minimum, hitboxMargin, hitboxMargin, hitboxMargin)
                .stretch(maximum.subtract(minimum)).expand(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityCollision(client.world, client.player,
                minimum, maximum, search, EntityPredicates.CAN_HIT, hitboxMargin);
        return entityHit != null ? entityHit.getPos() : maximum;
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        if (client.player == null || client.world == null) {
            return;
        }
        float tickProgress = event.getTickProgress();
        Vec3d origin = client.player.getCameraPosVec(tickProgress);
        ItemStack heldStack = client.player.getMainHandStack();
        AttackRangeComponent range = heldStack.get(DataComponentTypes.ATTACK_RANGE);
        if (range == null) {
            range = AttackRangeComponent.defaultForEntity(client.player);
        }
        boolean showServerAim = rotations().isSilentRotationActive();
        Vec3d serverEndpoint = null;
        if (showServerAim) {
            float[] angles = getServerAngles();
            float yaw = angles[0];
            float pitch = angles[1];
            serverEndpoint = findEndpoint(origin, pitch, yaw, range);
        }
        float clientYaw = client.player.getLerpedYaw(tickProgress);
        float clientPitch = client.player.getLerpedPitch(tickProgress);
        Vec3d clientEndpoint = findEndpoint(origin, clientPitch, clientYaw, range);
        int serverArgb = ColorHelper.fromFloats(serverColor.getAlphaFraction(),
                serverColor.getRedFraction(), serverColor.getGreenFraction(), serverColor.getBlueFraction());
        int clientArgb = ColorHelper.fromFloats(clientColor.getAlphaFraction(),
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
