package dev.echo.client.features.impl.movement;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.events.input.MouseMovementEvent;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.feature.movement.DiveBombSequence;
import dev.echo.client.feature.movement.ThrowInventoryOperations;
import dev.echo.client.features.Feature;
import dev.echo.client.handlers.AimTransition;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.handlers.RotationManager;
import dev.echo.client.handlers.RotationPriority;
import dev.echo.client.handlers.RotationSettings;
import dev.echo.client.handlers.RotationVisibility;
import dev.echo.client.mixin.MinecraftClientAccessor;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.setting.KeybindSetting;
import dev.echo.client.utils.ActionTimer;
import dev.echo.client.utils.OwnerSet;
import dev.echo.client.utils.items.InventoryItems;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

/** Coordinates ordinary and instant wind-charge/ender-pearl dive-bomb sequences. */
public class DiveBombFeature extends Feature {
    private static BooleanSetting autoJump;
    private static BooleanSetting silentAim;
    private static BooleanSetting switchback;
    private static IntSetting tickDelay;
    private static KeybindSetting diveBomb;
    private static KeybindSetting instantDiveBomb;

    private boolean throwActive;
    private boolean previousNormalPress;
    private boolean previousInstantPress;
    private ActionTimer throwTimer;
    private boolean rotationRequested;
    private int throwStage;
    private DiveBombSequence instantSequence;

    public DiveBombFeature() {
        super(new dev.echo.client.features.FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Dive Bomb"), dev.echo.client.bootstrap.CatalogText.literal("Tp to the sky with a wind charge and enderpearl"), dev.echo.client.features.Category.MOVEMENT));
    }

    @Override
    public void postInitialize() {
        throwActive = false;
        previousNormalPress = false;
        previousInstantPress = false;
        throwTimer = new ActionTimer();
        rotationRequested = false;
        throwStage = 0;
        instantSequence = null;
        tickDelay.tooltip("Timing");
    }

    @Override
    public void onDisable() {
        OwnerSet.remove(this);
        releaseRotationIfRequested();
        throwActive = false;
        previousNormalPress = false;
        previousInstantPress = false;
        instantSequence = null;
        DeferredActionQueue.finish(this, false);
        throwTimer.reset();
        throwStage = 0;
        super.onDisable();
    }

    @Subscribe
    public void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            return;
        }
        if (autoJump.getValue() && throwActive && instantSequence == null && client.player.isOnGround()) {
            event.jump = true;
        }
        if (throwActive) {
            event.forward = false;
            event.backward = false;
            event.left = false;
            event.right = false;
        }
        handleInstantPress();
        if (instantSequence != null) {
            if (instantSequence.tick()) {
                finishInstantSequence();
            }
            return;
        }
        if (!throwActive && (!hasRequiredHotbarItems() || hasItemCooldown())) {
            return;
        }
        boolean pressed = diveBomb.getKey().isPressed();
        if (pressed && !previousNormalPress) {
            if (rotations().snapshot().target() != null && !rotations().canRequest(this, RotationPriority.HIGH)) {
                return;
            }
            throwActive = true;
            throwStage = 0;
        }
        previousNormalPress = pressed;
        if (!throwActive || !rotations().isAiming()) {
            return;
        }
        float pitch = silentAim.getValue() ? rotations().effectiveRotation().pitch() : client.player.getPitch();
        if (pitch >= -89.5F) {
            return;
        }
        if (throwStage == 0) {
            if (hasItemCooldown()) {
                return;
            }
            int pearlSlot = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.ENDER_PEARL);
            if (pearlSlot == -1 || !DeferredActionQueue.ensureSelected(this, pearlSlot, -1, false)) {
                finishNormalSequence();
                return;
            }
            useItem();
            throwTimer.reset();
            throwStage = 1;
        } else if (throwStage == 1) {
            if (throwTimer.hasElapsedTicks(tickDelay.getValue())) {
                throwStage = 2;
            }
        } else if (throwStage == 2) {
            int chargeSlot = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
            if (chargeSlot == -1 || !DeferredActionQueue.ensureSelected(this, chargeSlot, -1, false)) {
                finishNormalSequence();
                return;
            }
            useItem();
            throwTimer.reset();
            throwStage = 3;
        } else if (throwStage == 3 && throwTimer.hasElapsedTicks(1)) {
            finishNormalSequence();
        }
    }

    private boolean hasRequiredHotbarItems() {
        return InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE) != -1
                && InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.ENDER_PEARL) != -1;
    }

    private boolean hasItemCooldown() {
        return client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL))
                || client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.WIND_CHARGE));
    }

    private void handleInstantPress() {
        boolean pressed = instantDiveBomb.getKey().isPressed();
        if (pressed && !previousInstantPress) {
            startInstantSequence();
        }
        previousInstantPress = pressed;
    }

    private void startInstantSequence() {
        if (throwActive || instantSequence != null || hasItemCooldown()) {
            return;
        }
        if (client.interactionManager == null || client.player.isSpectator()) {
            return;
        }
        int originalSlot = client.player.getInventory().getSelectedSlot();
        boolean moveCharge = !client.player.getOffHandStack().isOf(Items.WIND_CHARGE);
        int chargeSlot = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
        int pearlSlot = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.ENDER_PEARL);
        if (moveCharge && chargeSlot == -1) {
            return;
        }
        if (!client.player.getMainHandStack().isOf(Items.ENDER_PEARL) && pearlSlot == -1) {
            return;
        }
        if (rotations().snapshot().target() != null && !rotations().canRequest(this, RotationPriority.HIGH)) {
            return;
        }
        throwActive = true;
        throwStage = 0;
        OwnerSet.add(this);
        instantSequence = new DiveBombSequence(client, originalSlot, chargeSlot, pearlSlot, moveCharge,
                () -> ThrowInventoryOperations.swapHands(client), this::useItem,
                () -> silentAim.getValue() ? rotations().effectiveRotation().pitch() : client.player.getPitch());
    }

    private void finishInstantSequence() {
        OwnerSet.remove(this);
        instantSequence = null;
        throwActive = false;
        throwStage = 0;
        releaseRotationIfRequested();
    }

    private void finishNormalSequence() {
        throwActive = false;
        throwStage = 0;
        if (!switchback.getValue()) {
            DeferredActionQueue.finish(this, false);
        }
        releaseRotationIfRequested();
    }

    private void releaseRotationIfRequested() {
        if (!rotationRequested) {
            return;
        }
        if (silentAim.getValue() && rotations().beginReturn(this)) {
            return;
        }
        rotationRequested = false;
    }

    @Subscribe
    public void onMouseMovement(MouseMovementEvent.Post event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            return;
        }
        if (!throwActive && rotationRequested) {
            if (throwTimer.hasElapsedTicks(1)) {
                releaseRotationIfRequested();
            }
            return;
        }
        if (!throwActive) {
            return;
        }
        RotationManager manager = rotations();
        float yaw;
        if (manager.isAiming() && manager.isSilentRotationActive()) {
            yaw = manager.effectiveRotation().yaw();
        } else {
            yaw = silentAim.getValue() ? manager.effectiveRotation().yaw() : client.player.getYaw();
        }
        Vec3d aimPoint = client.player.getEyePos().add(Vec3d.fromPolar(-89.9F, yaw).multiply(10.0));
        manager.aim(this, RotationPriority.HIGH, aimPoint, RotationSettings.uniform(
                silentAim.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE,
                AimTransition.EASED, 450.0F));
        rotationRequested = true;
    }

    @Subscribe
    public void onHandleInput(HandleInputEvent.Pre event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            return;
        }
        if (!switchback.getValue()) {
            return;
        }
        if (!throwActive && DeferredActionQueue.contains(this) && throwTimer.hasElapsedTicks(1)) {
            DeferredActionQueue.finish(this);
        }
    }

    private void useItem() {
        ((MinecraftClientAccessor) client).invokeDoItemUse();
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(DiveBombFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, DiveBombFeature.autoJump);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, DiveBombFeature.silentAim);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, DiveBombFeature.switchback);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, DiveBombFeature.tickDelay);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, DiveBombFeature.diveBomb);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, DiveBombFeature.instantDiveBomb);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(DiveBombFeature feature, dev.echo.client.setting.IntSetting preparedTickDelay, dev.echo.client.setting.BooleanSetting preparedAutoJump, dev.echo.client.setting.KeybindSetting preparedInstantDiveBomb, dev.echo.client.setting.BooleanSetting preparedSilentAim, dev.echo.client.setting.KeybindSetting preparedDiveBomb, dev.echo.client.setting.BooleanSetting preparedSwitchback) {
        DiveBombFeature.tickDelay = preparedTickDelay;
        DiveBombFeature.autoJump = preparedAutoJump;
        DiveBombFeature.instantDiveBomb = preparedInstantDiveBomb;
        DiveBombFeature.silentAim = preparedSilentAim;
        DiveBombFeature.diveBomb = preparedDiveBomb;
        DiveBombFeature.switchback = preparedSwitchback;
    }

    /** Original shared control storage; independent of this feature's throw stage. */
    private static int sharedControlState;

    static {
        if (readSharedControlState() != 0) {
            writeSharedControlState(6);
        }
    }

    /** Reads the original mutable control field without assuming a fixed value. */
    public static int readSharedControlState() {
        return sharedControlState;
    }

    /** Preserves the original public static field writer. */
    public static void writeSharedControlState(int value) {
        sharedControlState = value;
    }

    /** Original derived branch value, with exactly one read of the shared state. */
    public static int getSharedControlFallbackValue() {
        int state = readSharedControlState();
        return state == 0 ? 73 : 0;
    }
}
