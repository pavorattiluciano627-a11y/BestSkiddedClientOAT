package dev.echo.client.features.impl.utility;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.events.input.MouseMovementEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.handlers.AimTransition;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.handlers.RotationPriority;
import dev.echo.client.handlers.RotationSettings;
import dev.echo.client.handlers.RotationVisibility;
import dev.echo.client.mixin.MinecraftClientAccessor;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.utils.ActionTimer;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.items.PotionItems;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

/** Healing/buff selection with independently sampled aiming, use and restoration delays. */
public class SmartPotFeature extends Feature {
    private static BooleanSetting angleCheck;
    private static FloatRangeSetting fov;
    private static FloatRangeSetting minHealth;
    private static BooleanSetting throwPotion;
    private static BooleanSetting autoAim;
    private static BooleanSetting silentAim;
    private static FloatRangeSetting throwDelay;
    private static BooleanSetting autoBuff;
    private static BooleanSetting swapBack;
    private static FloatRangeSetting swapBackDelay;
    private ActionTimer throwTimer;
    private ActionTimer restoreTimer;
    private int speedAttemptTick;
    private int strengthAttemptTick;
    private boolean aimingForPotion;
    private boolean ownsRotation;

    public SmartPotFeature() {
        super(new dev.echo.client.features.FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Smart Pot"), dev.echo.client.bootstrap.CatalogText.literal("Intelligent potion usage"), dev.echo.client.features.Category.UTILITY));
    }

    @Override
    public void postInitialize() {
        throwTimer = new ActionTimer();
        restoreTimer = new ActionTimer();
        speedAttemptTick = -1;
        strengthAttemptTick = -1;
        aimingForPotion = false;
        ownsRotation = false;
        fov.setVisibilityPredicate(ignored -> angleCheck.getValue());
        autoAim.setVisibilityPredicate(ignored -> throwPotion.getValue());
        silentAim.setVisibilityPredicate(ignored -> throwPotion.getValue() && autoAim.getValue());
        throwDelay.setVisibilityPredicate(ignored -> throwPotion.getValue());
        swapBackDelay.setVisibilityPredicate(ignored -> swapBack.getValue());
        throwDelay.tooltip("Timing");
        swapBackDelay.tooltip("Timing");
    }

    @Subscribe
    private void afterMouseMovement(MouseMovementEvent.Post event) {
        if (!autoAim.getValue() || !throwPotion.getValue() || !canAct()) {
            if (aimingForPotion) finishAim();
            return;
        }
        boolean needsAim = hasHealingPotion() && needsHealing();
        if (needsAim) {
            beginAim();
            aimDownward();
        } else if (aimingForPotion) {
            finishAim();
        }
    }

    @Subscribe
    private void beforeInput(HandleInputEvent.Pre event) {
        if (!canAct()) return;
        updatePotions();
    }

    private void updatePotions() {
        selectHealingPotion();
        selectBuffPotions();
        if (swapBack.getValue() && DeferredActionQueue.contains(this)
                && restoreTimer.hasElapsedMillis((double) swapBackDelay.sampleGaussian())) {
            restoreSlot();
        }
    }

    private void selectHealingPotion() {
        if (!canUseHealingPotion()) return;
        if (PotionItems.holdsHealingPotion()) {
            useHeldPotionIfEnabled();
        } else {
            int slot = PotionItems.findHealingPotion();
            if (slot != -1) selectPotion(slot);
        }
    }

    private void selectBuffPotions() {
        if (!autoBuff.getValue()) return;
        if (angleCheck.getValue() && client.player.getPitch() < 90.0F - fov.sampleUniform()) return;
        if (client.player.hasStatusEffect(StatusEffects.SPEED)
                && client.player.hasStatusEffect(StatusEffects.STRENGTH)) return;

        // These <= resets are literal original behavior, including repeated calls in the same tick.
        if (speedAttemptTick != -1 && client.player.age <= speedAttemptTick) speedAttemptTick = -1;
        if (strengthAttemptTick != -1 && client.player.age <= strengthAttemptTick) strengthAttemptTick = -1;
        if (!client.player.hasStatusEffect(StatusEffects.SPEED) && client.player.age > speedAttemptTick) {
            if (useMatchingHeldPotion(PotionItems.holdsSpeedPotion())) {
                speedAttemptTick = client.player.age;
            } else {
                int slot = PotionItems.findSpeedPotion();
                if (slot != -1 && selectPotion(slot)) speedAttemptTick = client.player.age;
            }
        }
        if (client.player.hasStatusEffect(StatusEffects.STRENGTH) || client.player.age <= strengthAttemptTick) return;
        if (useMatchingHeldPotion(PotionItems.holdsStrengthPotion())) {
            strengthAttemptTick = client.player.age;
        } else {
            int slot = PotionItems.findStrengthPotion();
            if (slot != -1 && selectPotion(slot)) strengthAttemptTick = client.player.age;
        }
    }

    private boolean canUseHealingPotion() {
        float minimumPitch = 90.0F - fov.sampleUniform();
        float pitch;
        if (autoAim.getValue() && silentAim.getValue()) {
            pitch = rotations().effectiveRotation().pitch();
        } else {
            pitch = client.player.getPitch();
        }
        boolean angleAllowed = !angleCheck.getValue() || pitch >= minimumPitch;
        return client.player.isOnGround() && needsHealing() && angleAllowed;
    }

    private boolean selectPotion(int slot) {
        if (swapBack.getValue()) {
            if (!DeferredActionQueue.ensureSelected(this, slot, -1, false, null)) return false;
        } else {
            InventoryItems.selectHotbarSlot(slot);
        }
        restoreTimer.reset();
        if (throwPotion.getValue()) {
            tryThrowPotion();
            return true;
        }
        // Selection still happened; the original return reports automatic use being enabled.
        return false;
    }

    private void tryThrowPotion() {
        if (!throwTimer.hasElapsedMillis((double) throwDelay.sampleGaussian())) return;
        throwTimer.reset();
        ((MinecraftClientAccessor) (Object) client).invokeDoItemUse();
    }

    private void useHeldPotionIfEnabled() {
        if (throwPotion.getValue()) tryThrowPotion();
    }

    private boolean useMatchingHeldPotion(boolean matches) {
        if (!matches) return false;
        useHeldPotionIfEnabled();
        return true;
    }

    private boolean canAct() {
        return !isOutsideWorld() && client.currentScreen == null;
    }

    private boolean hasHealingPotion() {
        return PotionItems.holdsHealingPotion() || PotionItems.findHealingPotion() != -1;
    }

    private boolean needsHealing() {
        return client.player.getHealth() <= minHealth.sampleUniform();
    }

    private void beginAim() {
        if (!aimingForPotion) aimingForPotion = true;
    }

    private void aimDownward() {
        float minimumPitch = 90.0F - fov.sampleUniform();
        Vec3d point = client.player.getEyePos().add(Vec3d.fromPolar(Math.max(minimumPitch, 89.9F), client.player.getYaw()));
        RotationSettings settings = RotationSettings.uniform(
                silentAim.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE,
                AimTransition.EASED, 450.0F);
        if (rotations().aim(this, RotationPriority.CRITICAL, point, settings)) ownsRotation = true;
    }

    private void finishAim() {
        if (!ownsRotation) {
            aimingForPotion = false;
            return;
        }
        if (silentAim.getValue() && rotations().beginReturn(this)) return;
        ownsRotation = false;
        aimingForPotion = false;
    }

    private void restoreSlot() {
        DeferredActionQueue.finish(this);
        restoreTimer.reset();
    }

    @Override
    public void onDisable() {
        DeferredActionQueue.finish(this, false);
        speedAttemptTick = -1;
        strengthAttemptTick = -1;
        aimingForPotion = false;
        ownsRotation = false;
        rotations().cancel(this);
        throwTimer.reset();
        restoreTimer.reset();
        super.onDisable();
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(SmartPotFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SmartPotFeature.angleCheck);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SmartPotFeature.fov);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SmartPotFeature.minHealth);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SmartPotFeature.throwPotion);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SmartPotFeature.autoAim);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SmartPotFeature.silentAim);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SmartPotFeature.throwDelay);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SmartPotFeature.autoBuff);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SmartPotFeature.swapBack);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SmartPotFeature.swapBackDelay);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(SmartPotFeature feature, dev.echo.client.setting.BooleanSetting preparedAutoBuff, dev.echo.client.setting.FloatRangeSetting preparedThrowDelay, dev.echo.client.setting.BooleanSetting preparedThrowPotion, dev.echo.client.setting.FloatRangeSetting preparedSwapBackDelay, dev.echo.client.setting.BooleanSetting preparedSwapBack, dev.echo.client.setting.BooleanSetting preparedSilentAim, dev.echo.client.setting.BooleanSetting preparedAngleCheck, dev.echo.client.setting.FloatRangeSetting preparedFov, dev.echo.client.setting.FloatRangeSetting preparedMinHealth, dev.echo.client.setting.BooleanSetting preparedAutoAim) {
        SmartPotFeature.autoBuff = preparedAutoBuff;
        SmartPotFeature.throwDelay = preparedThrowDelay;
        SmartPotFeature.throwPotion = preparedThrowPotion;
        SmartPotFeature.swapBackDelay = preparedSwapBackDelay;
        SmartPotFeature.swapBack = preparedSwapBack;
        SmartPotFeature.silentAim = preparedSilentAim;
        SmartPotFeature.angleCheck = preparedAngleCheck;
        SmartPotFeature.fov = preparedFov;
        SmartPotFeature.minHealth = preparedMinHealth;
        SmartPotFeature.autoAim = preparedAutoAim;
    }
}
