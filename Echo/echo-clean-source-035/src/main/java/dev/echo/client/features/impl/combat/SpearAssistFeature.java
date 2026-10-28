package dev.echo.client.features.impl.combat;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.MouseMovementEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.handlers.AimTransition;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.handlers.RotationPriority;
import dev.echo.client.handlers.RotationSettings;
import dev.echo.client.handlers.RotationVisibility;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.targeting.AimPointMode;
import dev.echo.client.utils.targeting.AimPointSettings;
import dev.echo.client.utils.targeting.EntityRaycasts;
import dev.echo.client.utils.targeting.TargetSelection;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Spear-use targeting with high-priority rotation ownership and deferred disable. */
public class SpearAssistFeature extends Feature {
    private static final CharSequence BLATANT = CatalogText.literal("Blatant");
    private static final CharSequence CLOSEST = CatalogText.literal("Closest");
    private static final CharSequence NATURAL = CatalogText.literal("Natural");
    private static ChoiceSetting aimMath;
    private static ChoiceSetting aimVector;
    private static ChoiceSetting targetPriority;
    private static BooleanSetting random;
    private static BooleanSetting silentRotations;
    private static BooleanSetting throughWalls;
    private static FloatSetting horizontalSpeed;
    private static FloatSetting verticalSpeed;
    private static FloatSetting range;
    private static FloatSetting fov;
    private static FloatSetting multipoint;
    private boolean waitingForRotationRelease;

    public SpearAssistFeature() {
        super(new FeatureInfo(CatalogText.literal("Spear Assist"),
                CatalogText.literal("Automatically aims at targets while holding a spear"), Category.COMBAT));
    }

    @Override
    public void postInitialize() {
        waitingForRotationRelease = false;
        horizontalSpeed.setVisibilityPredicate(value -> !aimMath.isSelected(BLATANT));
        verticalSpeed.setVisibilityPredicate(value -> !aimMath.isSelected(BLATANT));
        horizontalSpeed.tooltip("Timing");
        verticalSpeed.tooltip("Timing");
    }

    @Override
    public CharSequence getSuffix() {
        return aimMath.getValue();
    }

    @Override
    public void onEnable() {
        waitingForRotationRelease = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (silentRotations.getValue() && rotations().isOwnedBy(this)) {
            waitingForRotationRelease = true;
            return;
        }
        waitingForRotationRelease = false;
        rotations().cancel(this);
        super.onDisable();
    }

    @Subscribe
    private void onMouseMovement(MouseMovementEvent.Post event) {
        if (waitingForRotationRelease) {
            finishDisable();
            return;
        }
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            rotations().beginReturn(this);
            return;
        }
        if (!isUsingSpear()) {
            if (silentRotations.getValue() && rotations().isOwnedBy(this)
                    && (DeferredActionQueue.contains(MaceSwapFeature.class)
                            || DeferredActionQueue.contains(SpearReachFeature.class))) {
                return;
            }
            clearAim();
            return;
        }
        double distance = range.getValue();
        double distanceSquared = distance * distance;
        Vec3 eyes = client.player.getEyePosition(1.0F);
        List<LivingEntity> candidates = client.level.getEntitiesOfClass(LivingEntity.class,
                client.player.getBoundingBox().inflate(distance), entity -> isCandidate(eyes, distanceSquared, entity));
        LivingEntity selected = EntityRaycasts.selectTarget(client.player, candidates, eyes, targetPriority);
        LivingEntity target = TargetSelection.resolveTarget(selected);
        if (target == null || eyes.distanceToSqr(target.position()) > distanceSquared) {
            clearAim();
            return;
        }
        if (!isWithinFov(target)) {
            clearAim();
            return;
        }
        boolean aimed = rotations().aim(this, RotationPriority.HIGH, target,
                new AimPointSettings(aimVector.isSelected(CLOSEST) ? AimPointMode.CLOSEST : AimPointMode.STRAIGHT,
                        multipoint.getValue(), random.getValue(), throughWalls.getValue()),
                new RotationSettings(silentRotations.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE,
                        transition(), horizontalSpeed.getValue() * 20.0F, verticalSpeed.getValue() * 20.0F));
        if (!aimed) clearAim();
    }

    private void clearAim() {
        if (silentRotations.getValue()) rotations().beginReturn(this);
        else rotations().cancel(this);
    }

    private void finishDisable() {
        if (rotations().beginReturn(this)) return;
        waitingForRotationRelease = false;
        super.onDisable();
    }

    private AimTransition transition() {
        if (aimMath.isSelected(NATURAL)) return AimTransition.NATURAL;
        return aimMath.isSelected(BLATANT) ? AimTransition.SNAP : AimTransition.EASED;
    }

    private boolean isWithinFov(LivingEntity target) {
        return EntityRaycasts.isWithinFov(client.player, target, fov.getValue());
    }

    private boolean isUsingSpear() {
        if (isOutsideWorld() || !client.player.isUsingItem()) return false;
        ItemStack active = client.player.getUseItem();
        if (active.isEmpty()) return false;
        return InventoryItems.isSpear(active.getItem());
    }

    private boolean isCandidate(Vec3 eyes, double distanceSquared, LivingEntity entity) {
        return entity != client.player && !entity.isRemoved() && entity.isAlive()
                && TargetSelection.isValidTarget(entity)
                && eyes.distanceToSqr(entity.position()) <= distanceSquared && isWithinFov(entity);
    }

    public static void appendBootstrapSettings(SpearAssistFeature feature) {
        CatalogSettings.append(feature, aimMath);
        CatalogSettings.append(feature, aimVector);
        CatalogSettings.append(feature, targetPriority);
        CatalogSettings.append(feature, random);
        CatalogSettings.append(feature, silentRotations);
        CatalogSettings.append(feature, throughWalls);
        CatalogSettings.append(feature, horizontalSpeed);
        CatalogSettings.append(feature, verticalSpeed);
        CatalogSettings.append(feature, range);
        CatalogSettings.append(feature, fov);
        CatalogSettings.append(feature, multipoint);
    }

    public static void assignBootstrapSettings(SpearAssistFeature feature, ChoiceSetting preparedAimMath,
            BooleanSetting preparedThroughWalls, ChoiceSetting preparedAimVector, FloatSetting preparedMultipoint,
            FloatSetting preparedVerticalSpeed, ChoiceSetting preparedTargetPriority, FloatSetting preparedRange,
            FloatSetting preparedHorizontalSpeed, FloatSetting preparedFov, BooleanSetting preparedRandom,
            BooleanSetting preparedSilentRotations) {
        aimMath = preparedAimMath;
        throughWalls = preparedThroughWalls;
        aimVector = preparedAimVector;
        multipoint = preparedMultipoint;
        verticalSpeed = preparedVerticalSpeed;
        targetPriority = preparedTargetPriority;
        range = preparedRange;
        horizontalSpeed = preparedHorizontalSpeed;
        fov = preparedFov;
        random = preparedRandom;
        silentRotations = preparedSilentRotations;
    }
}
