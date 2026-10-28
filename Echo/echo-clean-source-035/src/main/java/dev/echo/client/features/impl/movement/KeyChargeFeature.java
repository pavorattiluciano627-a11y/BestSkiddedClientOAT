package dev.echo.client.features.impl.movement;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.events.input.MouseMovementEvent;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.handlers.AimTransition;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.handlers.RotationPriority;
import dev.echo.client.handlers.RotationSettings;
import dev.echo.client.handlers.RotationVisibility;
import dev.echo.client.input.KeybindingCache;
import dev.echo.client.mixin.MinecraftClientAccessor;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.KeybindSetting;
import dev.echo.client.utils.ActionTimer;
import dev.echo.client.utils.items.InventoryItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Activates a wind charge with optional downward aim, jump and hotbar restoration. */
public class KeyChargeFeature extends Feature {
    private static BooleanSetting autoJump;
    private static BooleanSetting autoAim;
    private static BooleanSetting silentAim;
    private static BooleanSetting switchback;
    private static BooleanSetting silentSwap;
    private static KeybindSetting activateKey;
    private boolean activationPending;
    private boolean previousActivationPressed;
    private ActionTimer chargeTimer;
    private boolean chargeRotationActive;

    public KeyChargeFeature() {
        super(new FeatureInfo(CatalogText.literal("Key Charge"), CatalogText.literal("Charge attacks with key"),
                Category.MOVEMENT));
    }

    public static void appendBootstrapSettings(KeyChargeFeature feature) {
        CatalogSettings.append(feature, autoJump);
        CatalogSettings.append(feature, autoAim);
        CatalogSettings.append(feature, silentAim);
        CatalogSettings.append(feature, switchback);
        CatalogSettings.append(feature, silentSwap);
        CatalogSettings.append(feature, activateKey);
    }

    public static void assignBootstrapSettings(KeyChargeFeature feature, BooleanSetting preparedSwitchback,
            KeybindSetting preparedActivateKey, BooleanSetting preparedSilentSwap, BooleanSetting preparedAutoJump,
            BooleanSetting preparedSilentAim, BooleanSetting preparedAutoAim) {
        switchback = preparedSwitchback;
        activateKey = preparedActivateKey;
        silentSwap = preparedSilentSwap;
        autoJump = preparedAutoJump;
        silentAim = preparedSilentAim;
        autoAim = preparedAutoAim;
    }

    @Override
    public void postInitialize() {
        activationPending = false;
        previousActivationPressed = false;
        chargeTimer = new ActionTimer();
        chargeRotationActive = false;
        silentAim.setVisibilityPredicate(ignored -> autoAim.getValue());
        silentSwap.setVisibilityPredicate(ignored -> switchback.getValue());
    }

    @Override
    public void onDisable() {
        if (chargeRotationActive && rotations().isOwnedBy(this)) rotations().cancel(this);
        chargeRotationActive = false;
        activationPending = false;
        previousActivationPressed = false;
        DeferredActionQueue.finish(this, false);
        chargeTimer.reset();
        super.onDisable();
    }

    private boolean hasWindCharge() {
        return InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE) != -1;
    }

    private void finishChargeRotation() {
        if (!chargeRotationActive) return;
        if (silentAim.getValue() && rotations().beginReturn(this)) return;
        chargeRotationActive = false;
    }

    @Subscribe
    public void onPlayerInput(PlayerInputEvent event) {
        // The original hotbar search precedes the world guard.
        if (!hasWindCharge()) return;
        if (isOutsideWorld()) return;
        if (client.gui.screen() != null) return;
        event.jump = KeybindingCache.isPressed(client.options.keyJump);
        boolean activationPressed = activateKey.getKey().isPressed();
        if (activationPressed && !previousActivationPressed) {
            if (rotations().snapshot().target() != null && !rotations().canRequest(this, RotationPriority.CRITICAL)) return;
            activationPending = true;
        }
        previousActivationPressed = activationPressed;
        if (!activationPending) return;
        if (client.player.getCooldowns().isOnCooldown(new ItemStack(Items.WIND_CHARGE))) return;
        if (autoAim.getValue()) {
            if (!rotations().isAiming()) return;
            float pitch = silentAim.getValue() ? rotations().effectiveRotation().pitch() : client.player.getXRot();
            if (pitch < 88.0F) return;
        }
        int slot = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
        if (slot == -1) return;
        if (!DeferredActionQueue.ensureSelected(this, slot, -1, false)) return;
        ((MinecraftClientAccessor) client).invokeDoItemUse();
        if (silentSwap.getValue()) {
            DeferredActionQueue.finish(this, switchback.getValue());
        } else if (!switchback.getValue()) {
            DeferredActionQueue.finish(this, false);
        }
        chargeTimer.reset();
        if (autoJump.getValue() && client.player.onGround()
                && (autoAim.getValue() || !(client.player.getXRot() < 80.0F))) {
            event.jump = true;
        }
        activationPending = false;
    }

    @Subscribe
    public void onMouseMovement(MouseMovementEvent.Post event) {
        if (isOutsideWorld()) return;
        if (client.gui.screen() != null) return;
        if (!hasWindCharge() && chargeRotationActive) {
            if (!chargeTimer.hasElapsedTicks(1)) return;
            finishChargeRotation();
            return;
        }
        if (!hasWindCharge()) return;
        if (!autoAim.getValue()) return;
        if (activationPending) {
            float yaw;
            if (rotations().isAiming() && rotations().isSilentRotationActive()) {
                yaw = rotations().effectiveRotation().yaw();
            } else {
                yaw = silentAim.getValue() ? rotations().effectiveRotation().yaw() : client.player.getYRot();
            }
            Vec3 aimPoint = client.player.getEyePosition().add(Vec3.directionFromRotation(89.9F, yaw));
            rotations().aim(this, RotationPriority.CRITICAL, aimPoint,
                    RotationSettings.uniform(silentAim.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE,
                            AimTransition.EASED, 450.0F));
            // The original records the attempt even when the rotation request is refused.
            chargeRotationActive = true;
        } else if (chargeRotationActive) {
            finishChargeRotation();
        }
    }

    @Subscribe
    public void beforeInput(HandleInputEvent.Pre event) {
        if (isOutsideWorld()) return;
        if (client.gui.screen() != null) return;
        if (!switchback.getValue() || silentSwap.getValue()) return;
        if (!DeferredActionQueue.contains(this)) return;
        if (chargeTimer.hasElapsedTicks(1)) DeferredActionQueue.finish(this);
    }

    public static boolean hasPendingSlotChange() {
        return DeferredActionQueue.contains(KeyChargeFeature.class);
    }
}
