package dev.echo.client.features.impl.utility;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.MouseMovementEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.network.PacketReceiveEvent;
import dev.echo.client.events.network.PacketSendEvent;
import dev.echo.client.feature.movement.ThrowInventoryOperations;
import dev.echo.client.feature.utility.PearlCatchCalculations;
import dev.echo.client.feature.utility.PearlCatchInstantPhase;
import dev.echo.client.feature.utility.PearlCatchMode;
import dev.echo.client.feature.utility.PearlCatchStage;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.handlers.AimTransition;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.handlers.RotationPriority;
import dev.echo.client.handlers.RotationScope;
import dev.echo.client.handlers.RotationSettings;
import dev.echo.client.handlers.RotationVisibility;
import dev.echo.client.mixin.MinecraftClientAccessor;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.KeybindSetting;
import dev.echo.client.utils.OwnerSet;
import dev.echo.client.utils.Rotation;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.trajectory.LaunchCandidate;
import dev.echo.client.utils.trajectory.LaunchTrajectoryMath;
import dev.echo.client.utils.trajectory.LaunchTrajectorySearch;
import dev.echo.client.utils.trajectory.ProjectileState;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Normal and instant pearl/wind-charge sequences. Conditional source candidate; see receipt.json. */
public class PearlCatchFeature extends Feature {
    private static final double LAUNCH_HEIGHT_OFFSET = 0.1;
    private static final double SPAWN_MATCH_DISTANCE = 0.05;
    private static final double REQUIRED_HIT_FRACTION = 0.5;
    private static final double LAST_CHANCE_HIT_FRACTION = 0.2;
    private static final int AIM_WAIT_TICKS = 8;
    private static final int RETRY_DELAY_TICKS = 10;
    private static final int MAX_RETRIES = 2;
    private static final int CONFIRM_GRACE_TICKS = 3;
    private static final float MIN_AIM_SPEED = 40.0F;
    private static final float MAX_AIM_SPEED = 300.0F;
    private static final float IMMEDIATE_AIM_SPEED = 1000.0F;
    private static final double VISIBLE_AIM_TOLERANCE = 0.1;
    private static final double AIM_DISTANCE = 100.0;
    private static final String HIGH_DELAY_TOOLTIP = "Timing";

    private static BooleanSetting autoAim;
    private static BooleanSetting silentAim;
    private static FloatRangeSetting highDelay;
    private static BooleanSetting retry;
    private static BooleanSetting switchBack;
    private static KeybindSetting highPearlCatch;
    private static KeybindSetting lowPearlCatch;
    private static KeybindSetting instantPearlCatch;

    private PearlCatchStage normalStage;
    private PearlCatchInstantPhase instantPhase;
    private boolean previousHighPress;
    private boolean previousLowPress;
    private boolean previousInstantPress;
    private int normalStageTicks;
    private int pearlThrowTick;
    private int scheduledCatchTick;
    private int catchDelayTicks;
    private boolean lowTrajectory;
    private int pearlFlightTicks;
    private ProjectileState trajectory;
    private LaunchCandidate pendingPlan;
    private LaunchCandidate activePlan;
    private Vec3 planningOrigin;
    private Vec3 planningVelocity;
    private int planningElapsedTicks;
    private int plannedPlayerTick;
    private volatile boolean awaitingPearlSpawn;
    private volatile Vec3 pearlSpawnPosition;
    private volatile Vec3 pearlSpawnVelocity;
    private volatile int pearlSpawnPlayerAge;
    private volatile int pearlEntityId;
    private volatile boolean pearlDestroyed;
    private volatile boolean awaitingWindChargeSpawn;
    private volatile Vec3 windChargeSpawnPosition;
    private volatile Vec3 windChargeSpawnVelocity;
    private volatile boolean positionCorrectionReceived;
    private LaunchCandidate thrownPlan;
    private int windChargeThrowTick;
    private int retryCount;
    private boolean retryPending;
    private Vec3 capturedOrigin;
    private Vec3 capturedVelocity;
    private boolean capturedOnGround;
    private boolean movementPacketSent;
    private int originalSlot;
    private int windChargeSlot;
    private int pearlSlot;
    private boolean movedWindChargeToOffhand;
    private int instantPhaseTick;

    public PearlCatchFeature() {
        super(new FeatureInfo(CatalogText.literal("Pearl Catch"),
                CatalogText.literal("Automatically catches ender pearls thrown by the player"), Category.UTILITY));
    }

    @Override
    public void postInitialize() {
        normalStage = PearlCatchStage.IDLE;
        instantPhase = PearlCatchInstantPhase.IDLE;
        previousHighPress = false;
        previousLowPress = false;
        previousInstantPress = false;
        normalStageTicks = 0;
        pearlThrowTick = -1;
        scheduledCatchTick = -1;
        catchDelayTicks = 1;
        lowTrajectory = false;
        pearlFlightTicks = 0;
        plannedPlayerTick = -1;
        awaitingPearlSpawn = false;
        pearlSpawnPlayerAge = -1;
        pearlEntityId = -1;
        pearlDestroyed = false;
        awaitingWindChargeSpawn = false;
        positionCorrectionReceived = false;
        windChargeThrowTick = -1;
        retryCount = 0;
        retryPending = false;
        capturedVelocity = Vec3.ZERO;
        capturedOnGround = true;
        movementPacketSent = false;
        originalSlot = -1;
        windChargeSlot = -1;
        pearlSlot = -1;
        movedWindChargeToOffhand = false;
        instantPhaseTick = -1;
        silentAim.setVisibilityPredicate(ignored -> autoAim.getValue());
        retry.setVisibilityPredicate(ignored -> autoAim.getValue());
        highDelay.tooltip(HIGH_DELAY_TOOLTIP);
    }

    @Subscribe
    public void onTick(ClientTickEvent event) {
        if (isOutsideWorld() || client.gui.screen() != null) return;
        handleKeyPresses();
        if (instantPhase != PearlCatchInstantPhase.IDLE) {
            advanceInstantSequence();
            return;
        }
        switch (normalStage) {
            case THROW_PEARL -> handlePearlThrow();
            case WAIT -> handleWait();
            case CONFIRM -> handleConfirm();
            case SWITCH_BACK -> handleSwitchBack();
        }
    }

    @Subscribe
    public void onPacketSent(PacketSendEvent event) {
        if (client.player == null) return;
        if (event.getPacket() instanceof ServerboundMovePlayerPacket movement) {
            if (movement.hasPosition()) {
                Vec3 position = new Vec3(movement.getX(0.0), movement.getY(0.0), movement.getZ(0.0));
                capturedVelocity = capturedOrigin == null ? Vec3.ZERO : position.subtract(capturedOrigin);
                capturedOrigin = position;
            } else {
                capturedVelocity = Vec3.ZERO;
            }
            capturedOnGround = movement.isOnGround();
            movementPacketSent = true;
            return;
        }
        if (event.getPacket() instanceof ServerboundClientTickEndPacket) {
            if (!movementPacketSent) capturedVelocity = Vec3.ZERO;
            movementPacketSent = false;
            if (needsCatchPlan() && client.player.tickCount >= scheduledCatchTick) {
                prepareCatchPlan();
                return;
            }
            if (needsCatchPlan() && usesVisibleAim()) activePlan = planForTick(scheduledCatchTick);
            return;
        }
        if (event.getPacket() instanceof ServerboundAcceptTeleportationPacket) {
            capturedOrigin = null;
            capturedVelocity = Vec3.ZERO;
        }
    }

    @Subscribe
    public void onPacketReceived(PacketReceiveEvent event) {
        if (normalStage == PearlCatchStage.IDLE && instantPhase == PearlCatchInstantPhase.IDLE) return;
        observeIncoming(event.getPacket());
    }

    private void observeIncoming(Packet<?> packet) {
        if (packet instanceof ClientboundBundlePacket bundle) {
            for (Packet<?> inner : bundle.subPackets()) observeIncoming(inner);
            return;
        }
        if (packet instanceof ClientboundPlayerPositionPacket) {
            positionCorrectionReceived = true;
            return;
        }
        if (packet instanceof ClientboundRemoveEntitiesPacket destroyed) {
            if (pearlEntityId != -1 && destroyed.getEntityIds().contains(pearlEntityId)) pearlDestroyed = true;
            return;
        }
        observeSpawn(packet);
    }

    private void observeSpawn(Packet<?> packet) {
        if (!(packet instanceof ClientboundAddEntityPacket spawn)) return;
        if (client.player == null || spawn.getData() != client.player.getId()) return;
        Vec3 position = new Vec3(spawn.getX(), spawn.getY(), spawn.getZ());
        if (awaitingPearlSpawn && spawn.getType() == EntityTypes.ENDER_PEARL) {
            pearlSpawnPosition = position;
            pearlSpawnVelocity = spawn.getMovement();
            pearlSpawnPlayerAge = client.player.tickCount;
            pearlEntityId = spawn.getId();
            awaitingPearlSpawn = false;
        } else if (awaitingWindChargeSpawn && spawn.getType() == EntityTypes.WIND_CHARGE) {
            windChargeSpawnPosition = position;
            windChargeSpawnVelocity = spawn.getMovement();
            awaitingWindChargeSpawn = false;
        }
    }

    @Subscribe
    public void afterMouseMovement(MouseMovementEvent.Post event) {
        if (isOutsideWorld() || !usesVisibleAim() || !needsCatchPlan() || activePlan == null) return;
        Vec3 target = client.player.getEyePosition().add(
                LaunchTrajectoryMath.direction(activePlan.yaw(), activePlan.pitch()).scale(AIM_DISTANCE));
        boolean immediate = !retryPending && scheduledCatchTick - pearlThrowTick <= 1
                && scheduledCatchTick - client.player.tickCount <= 1;
        RotationSettings settings = immediate
                ? RotationSettings.uniform(RotationVisibility.VISIBLE, AimTransition.EASED, IMMEDIATE_AIM_SPEED)
                : RotationSettings.uniform(RotationVisibility.VISIBLE, AimTransition.NATURAL, aimSpeed());
        rotations().aim(this, RotationPriority.HIGH, target, settings);
    }

    private float aimSpeed() {
        return PearlCatchCalculations.aimSpeed(activePlan.yaw(), activePlan.pitch(), client.player.getYRot(),
                client.player.getXRot(), scheduledCatchTick, client.player.tickCount);
    }

    private boolean needsCatchPlan() {
        return normalStage == PearlCatchStage.WAIT
                || normalStage == PearlCatchStage.CONFIRM && retryPending
                || instantPhase == PearlCatchInstantPhase.THROW_FOLLOW_UP;
    }

    private boolean usesVisibleAim() { return autoAim.getValue() && !silentAim.getValue(); }

    @Override
    public void onDisable() {
        OwnerSet.remove(this);
        resetNormalSequence(false);
        resetInstantSequence();
        capturedOrigin = null;
        capturedVelocity = Vec3.ZERO;
        super.onDisable();
    }

    private void handleKeyPresses() {
        boolean highPressed = highPearlCatch.getKey().isPressed();
        boolean lowPressed = lowPearlCatch.getKey().isPressed();
        boolean instantPressed = instantPearlCatch.getKey().isPressed();
        if (highPressed && !previousHighPress) startNormalSequence(PearlCatchMode.HIGH);
        if (lowPressed && !previousLowPress) startNormalSequence(PearlCatchMode.LOW);
        if (instantPressed && !previousInstantPress) startInstantSequence();
        previousHighPress = highPressed;
        previousLowPress = lowPressed;
        previousInstantPress = instantPressed;
    }

    private void startNormalSequence(PearlCatchMode mode) {
        if (normalStage != PearlCatchStage.IDLE || instantPhase != PearlCatchInstantPhase.IDLE) return;
        if (isThrowCoolingDown()) return;
        int pearl = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.ENDER_PEARL);
        if (pearl == -1) return;
        int windCharge = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
        if (windCharge == -1) return;
        if (!DeferredActionQueue.ensureSelected(this, pearl, -1, false)) return;
        catchDelayTicks = mode == PearlCatchMode.HIGH ? Math.max(1, Math.round(highDelay.sampleGaussian())) : 1;
        lowTrajectory = mode == PearlCatchMode.LOW;
        normalStage = PearlCatchStage.THROW_PEARL;
        normalStageTicks = 0;
    }

    private void handlePearlThrow() {
        int pearl = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.ENDER_PEARL);
        if (pearl == -1 || !DeferredActionQueue.ensureSelected(this, pearl, -1, false)) {
            resetNormalSequence(false);
            return;
        }
        Vec3 origin = sequenceOrigin();
        Vec3 velocity = sequenceVelocity();
        Rotation rotation = rotations().effectiveRotation();
        pearlSpawnPosition = null;
        pearlSpawnVelocity = null;
        pearlSpawnPlayerAge = -1;
        pearlEntityId = -1;
        pearlDestroyed = false;
        positionCorrectionReceived = false;
        retryCount = 0;
        awaitingPearlSpawn = true;
        ((MinecraftClientAccessor) client).invokeDoItemUse();
        if (!client.player.getCooldowns().isOnCooldown(new ItemStack(Items.ENDER_PEARL))) {
            resetNormalSequence(true);
            return;
        }
        Vec3 launchPosition = new Vec3(origin.x, origin.y + client.player.getEyeHeight()
                - LAUNCH_HEIGHT_OFFSET, origin.z);
        trajectory = new ProjectileState(launchPosition,
                LaunchTrajectoryMath.direction(rotation.yaw(), rotation.pitch()).scale(1.5).add(velocity), false);
        pearlFlightTicks = LaunchTrajectorySearch.flightTicks(trajectory);
        pearlThrowTick = client.player.tickCount;
        scheduledCatchTick = pearlThrowTick + catchDelayTicks;
        normalStage = PearlCatchStage.WAIT;
    }

    private void prepareCatchPlan() {
        reconcilePearlSpawn();
        Vec3 origin = sequenceOrigin();
        planningOrigin = new Vec3(origin.x, origin.y + client.player.getEyeHeight(), origin.z);
        planningVelocity = sequenceVelocity();
        planningElapsedTicks = client.player.tickCount - pearlThrowTick;
        pendingPlan = autoAim.getValue() ? LaunchTrajectorySearch.findCatch(trajectory, planningOrigin,
                planningVelocity, planningElapsedTicks, pearlFlightTicks, lowTrajectory) : null;
        plannedPlayerTick = client.player.tickCount;
        if (pendingPlan != null) activePlan = pendingPlan;
    }

    private LaunchCandidate planForTick(int tick) {
        reconcilePearlSpawn();
        Vec3 origin = sequenceOrigin();
        Vec3 eye = new Vec3(origin.x, origin.y + client.player.getEyeHeight(), origin.z);
        LaunchCandidate plan = LaunchTrajectorySearch.findCatch(trajectory, eye, sequenceVelocity(),
                tick - pearlThrowTick, pearlFlightTicks, lowTrajectory);
        return plan != null ? plan : activePlan;
    }

    private LaunchCandidate usablePlan(boolean lastChance) {
        LaunchCandidate plan = pendingPlan;
        if (plan == null) return null;
        if (!usesVisibleAim()) {
            if (plan.hitFraction() < REQUIRED_HIT_FRACTION
                    && (!lastChance || plan.hitFraction() < LAST_CHANCE_HIT_FRACTION)) return null;
            return plan;
        }
        float yaw = client.player.getYRot();
        float pitch = client.player.getXRot();
        double hitFraction = LaunchTrajectorySearch.hitFraction(trajectory, planningOrigin, planningVelocity,
                planningElapsedTicks, yaw, pitch, pearlFlightTicks - 1);
        if (hitFraction < Math.max(REQUIRED_HIT_FRACTION, plan.hitFraction() - VISIBLE_AIM_TOLERANCE)
                && (!lastChance || hitFraction < LAST_CHANCE_HIT_FRACTION)) return null;
        return new LaunchCandidate(yaw, pitch, hitFraction, hitFraction, plan.meanHitTick());
    }

    private void useWithPlan(LaunchCandidate plan) {
        if (!silentAim.getValue()) {
            ((MinecraftClientAccessor) client).invokeDoItemUse();
            return;
        }
        rotations().override(this, RotationPriority.HIGH, new Rotation(plan.yaw(), plan.pitch()))
                .ifPresent(RotationScope::commit);
        useIgnoringCrosshair();
    }

    private void handleWait() {
        int slot = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
        if (slot == -1) {
            resetNormalSequence(true);
            return;
        }
        if (client.player.tickCount < scheduledCatchTick || plannedPlayerTick != client.player.tickCount) return;
        int lateTicks = client.player.tickCount - scheduledCatchTick;
        if (!autoAim.getValue()) {
            throwWindCharge(slot, null);
            return;
        }
        LaunchCandidate plan = usablePlan(lateTicks >= AIM_WAIT_TICKS);
        if (plan != null) throwWindCharge(slot, plan);
        else if (lateTicks >= AIM_WAIT_TICKS || client.player.tickCount - pearlThrowTick >= pearlFlightTicks)
            resetNormalSequence(true);
    }

    private void handleConfirm() {
        int tick = client.player.tickCount;
        if (positionCorrectionReceived || pearlDestroyed || tick - pearlThrowTick >= pearlFlightTicks) {
            resetNormalSequence(switchBack.getValue());
            return;
        }
        if (switchBack.getValue() && !retryPending && tick > windChargeThrowTick
                && DeferredActionQueue.contains(this)) DeferredActionQueue.finish(this, true);
        boolean missed = false;
        Vec3 position = windChargeSpawnPosition;
        Vec3 velocity = windChargeSpawnVelocity;
        if (position != null && velocity != null) {
            int delay = windChargeThrowTick - pearlThrowTick;
            missed = LaunchTrajectorySearch.interceptTick(trajectory, position, velocity, delay, pearlFlightTicks - 1) < 0;
        }
        int latencyTicks = pearlSpawnPlayerAge >= 0 ? Math.max(0, pearlSpawnPlayerAge - pearlThrowTick) : 2;
        boolean expired = tick > pearlThrowTick + thrownPlan.meanHitTick() + latencyTicks + CONFIRM_GRACE_TICKS;
        if (!missed && !expired) return;
        retryPending = true;
        if (!retry.getValue() || retryCount >= MAX_RETRIES) {
            resetNormalSequence(switchBack.getValue());
            return;
        }
        if (plannedPlayerTick != tick) return;
        if (client.player.getCooldowns().isOnCooldown(new ItemStack(Items.WIND_CHARGE))) return;
        int slot = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
        LaunchCandidate plan = usablePlan(true);
        if (slot == -1 || plan == null) {
            if (tick - windChargeThrowTick > 18) resetNormalSequence(switchBack.getValue());
            return;
        }
        retryCount++;
        throwWindCharge(slot, plan);
    }

    private void reconcilePearlSpawn() {
        Vec3 position = pearlSpawnPosition;
        Vec3 velocity = pearlSpawnVelocity;
        if (trajectory.exactVelocity() || position == null || velocity == null) return;
        if (position.distanceTo(trajectory.position()) > SPAWN_MATCH_DISTANCE) return;
        trajectory = new ProjectileState(trajectory.position(), velocity, true);
        pearlFlightTicks = LaunchTrajectorySearch.flightTicks(trajectory);
    }

    private void throwWindCharge(int slot, LaunchCandidate plan) {
        if (!ensureWindChargeSlot(slot)) {
            resetNormalSequence(true);
            return;
        }
        windChargeSpawnPosition = null;
        windChargeSpawnVelocity = null;
        awaitingWindChargeSpawn = plan != null;
        if (plan != null) useWithPlan(plan);
        else ((MinecraftClientAccessor) client).invokeDoItemUse();
        activePlan = null;
        if (plan != null) {
            thrownPlan = plan;
            retryPending = false;
            windChargeThrowTick = client.player.tickCount;
            scheduledCatchTick = windChargeThrowTick + RETRY_DELAY_TICKS;
            normalStage = PearlCatchStage.CONFIRM;
        } else if (switchBack.getValue()) {
            normalStage = PearlCatchStage.SWITCH_BACK;
            normalStageTicks = 0;
        } else resetNormalSequence(false);
    }

    private void useIgnoringCrosshair() {
        HitResult previousHit = client.hitResult;
        Vec3 eye = client.player.getEyePosition();
        client.hitResult = BlockHitResult.miss(eye, Direction.UP, client.player.blockPosition());
        ((MinecraftClientAccessor) client).invokeDoItemUse();
        // The artifact restores only on normal return, not in a finally block.
        client.hitResult = previousHit;
    }

    private void handleSwitchBack() {
        normalStageTicks++;
        if (normalStageTicks < 1) return;
        resetNormalSequence(switchBack.getValue());
    }

    private boolean ensureWindChargeSlot(int slot) {
        if (DeferredActionQueue.selectedSlot(this) == slot) return true;
        if (DeferredActionQueue.select(this, slot, -1, false)) return true;
        return client.player.getInventory().getSelectedSlot() == slot;
    }

    private Vec3 sequenceOrigin() {
        return capturedOrigin != null ? capturedOrigin : client.player.position();
    }

    private Vec3 sequenceVelocity() {
        return PearlCatchCalculations.restoredVelocity(capturedVelocity, capturedOnGround);
    }

    private void resetNormalSequence(boolean restore) {
        DeferredActionQueue.finish(this, restore);
        normalStage = PearlCatchStage.IDLE;
        normalStageTicks = 0;
        pearlThrowTick = -1;
        scheduledCatchTick = -1;
        catchDelayTicks = 1;
        lowTrajectory = false;
        trajectory = null;
        pendingPlan = null;
        activePlan = null;
        plannedPlayerTick = -1;
        awaitingPearlSpawn = false;
        awaitingWindChargeSpawn = false;
        thrownPlan = null;
        windChargeThrowTick = -1;
        retryCount = 0;
        retryPending = false;
    }

    private void startInstantSequence() {
        if (normalStage != PearlCatchStage.IDLE || instantPhase != PearlCatchInstantPhase.IDLE) return;
        if (isThrowCoolingDown()) return;
        if (client.gameMode == null || client.player.isSpectator()) return;
        int selected = client.player.getInventory().getSelectedSlot();
        int pearl = client.player.getMainHandItem().is(Items.ENDER_PEARL) ? selected
                : InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.ENDER_PEARL);
        if (pearl == -1) return;
        boolean swap = !client.player.getOffhandItem().is(Items.WIND_CHARGE);
        int windCharge = swap ? InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE) : -1;
        if (swap && windCharge == -1) return;
        OwnerSet.add(this);
        originalSlot = selected;
        windChargeSlot = windCharge;
        pearlSlot = pearl;
        movedWindChargeToOffhand = swap;
        if (swap) enterInstantPhase(PearlCatchInstantPhase.SELECT_WIND_CHARGE);
        else if (pearl != selected) enterInstantPhase(PearlCatchInstantPhase.SELECT_PEARL);
        else enterInstantPhase(PearlCatchInstantPhase.THROW);
        instantPhaseTick = client.player.tickCount;
        advanceInstantSequence();
    }

    private void advanceInstantSequence() {
        if (client.player.tickCount < instantPhaseTick) return;
        switch (instantPhase) {
            case SELECT_WIND_CHARGE -> {
                InventoryItems.selectHotbarSlot(windChargeSlot);
                enterInstantPhase(PearlCatchInstantPhase.SWAP_OFFHAND);
            }
            case SWAP_OFFHAND -> {
                swapHands();
                enterInstantPhase(PearlCatchInstantPhase.SELECT_PEARL);
            }
            case SELECT_PEARL -> {
                InventoryItems.selectHotbarSlot(pearlSlot);
                enterInstantPhase(PearlCatchInstantPhase.THROW);
            }
            case THROW -> throwInstant();
            case THROW_FOLLOW_UP -> throwInstantFollowUp();
            case RESTORE_SELECT_WIND_CHARGE -> {
                InventoryItems.selectHotbarSlot(windChargeSlot);
                enterInstantPhase(PearlCatchInstantPhase.RESTORE_SWAP_OFFHAND);
            }
            case RESTORE_SWAP_OFFHAND -> {
                swapHands();
                enterInstantPhase(PearlCatchInstantPhase.RESTORE_SLOT);
            }
            case RESTORE_SLOT -> {
                InventoryItems.selectHotbarSlot(originalSlot);
                resetInstantSequence();
            }
            default -> resetInstantSequence();
        }
    }

    private void throwInstant() {
        if (!client.player.getMainHandItem().is(Items.ENDER_PEARL)
                || !client.player.getOffhandItem().is(Items.WIND_CHARGE) || isThrowCoolingDown()) {
            beginInstantRestoration();
            return;
        }
        Vec3 origin = sequenceOrigin();
        Vec3 velocity = sequenceVelocity();
        Rotation rotation = rotations().effectiveRotation();
        Vec3 launchPosition = new Vec3(origin.x,
                origin.y + client.player.getEyeHeight() - LAUNCH_HEIGHT_OFFSET, origin.z);
        ProjectileState launched = new ProjectileState(launchPosition,
                LaunchTrajectoryMath.direction(rotation.yaw(), rotation.pitch()).scale(1.5).add(velocity), false);
        int flightTicks = LaunchTrajectorySearch.flightTicks(launched);
        Vec3 eye = new Vec3(origin.x, origin.y + client.player.getEyeHeight(), origin.z);
        double hitFraction = LaunchTrajectorySearch.hitFraction(launched, eye, velocity, 0,
                rotation.yaw(), rotation.pitch(), flightTicks - 1);
        if (!autoAim.getValue() || hitFraction >= REQUIRED_HIT_FRACTION) {
            ((MinecraftClientAccessor) client).invokeDoItemUse();
            ((MinecraftClientAccessor) client).invokeDoItemUse();
            usePendingOffhand();
            beginInstantRestoration();
            return;
        }
        pearlSpawnPosition = null;
        pearlSpawnVelocity = null;
        pearlSpawnPlayerAge = -1;
        awaitingPearlSpawn = true;
        ((MinecraftClientAccessor) client).invokeDoItemUse();
        if (!client.player.getCooldowns().isOnCooldown(new ItemStack(Items.ENDER_PEARL))) {
            beginInstantRestoration();
            return;
        }
        trajectory = launched;
        pearlFlightTicks = flightTicks;
        lowTrajectory = true;
        pearlThrowTick = client.player.tickCount;
        scheduledCatchTick = pearlThrowTick + 1;
        enterInstantPhase(PearlCatchInstantPhase.THROW_FOLLOW_UP);
    }

    private void throwInstantFollowUp() {
        int tick = client.player.tickCount;
        if (plannedPlayerTick != tick) return;
        LaunchCandidate plan = usablePlan(tick - scheduledCatchTick >= AIM_WAIT_TICKS);
        if (plan != null && client.player.getOffhandItem().is(Items.WIND_CHARGE)) {
            useWithPlan(plan);
            usePendingOffhand();
            finishInstantFollowUp();
        } else if (tick - scheduledCatchTick >= AIM_WAIT_TICKS || tick - pearlThrowTick >= pearlFlightTicks) {
            finishInstantFollowUp();
        }
    }

    private void finishInstantFollowUp() {
        trajectory = null;
        pendingPlan = null;
        activePlan = null;
        plannedPlayerTick = -1;
        awaitingPearlSpawn = false;
        beginInstantRestoration();
    }

    private void usePendingOffhand() { PearlCatchCalculations.usePendingOffhandWindCharge(client); }

    private void beginInstantRestoration() {
        if (movedWindChargeToOffhand) enterInstantPhase(PearlCatchInstantPhase.RESTORE_SELECT_WIND_CHARGE);
        else if (originalSlot != client.player.getInventory().getSelectedSlot())
            enterInstantPhase(PearlCatchInstantPhase.RESTORE_SLOT);
        else resetInstantSequence();
    }

    private void enterInstantPhase(PearlCatchInstantPhase phase) {
        instantPhase = phase;
        instantPhaseTick = client.player.tickCount + 1;
    }

    private void swapHands() { ThrowInventoryOperations.swapHands(client); }

    private boolean isThrowCoolingDown() {
        return client.player.getCooldowns().isOnCooldown(new ItemStack(Items.ENDER_PEARL))
                || client.player.getCooldowns().isOnCooldown(new ItemStack(Items.WIND_CHARGE));
    }

    private void resetInstantSequence() {
        OwnerSet.remove(this);
        instantPhase = PearlCatchInstantPhase.IDLE;
        instantPhaseTick = -1;
        originalSlot = -1;
        windChargeSlot = -1;
        pearlSlot = -1;
        movedWindChargeToOffhand = false;
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(PearlCatchFeature feature) {
        CatalogSettings.append(feature, autoAim);
        CatalogSettings.append(feature, silentAim);
        CatalogSettings.append(feature, highDelay);
        CatalogSettings.append(feature, retry);
        CatalogSettings.append(feature, switchBack);
        CatalogSettings.append(feature, highPearlCatch);
        CatalogSettings.append(feature, lowPearlCatch);
        CatalogSettings.append(feature, instantPearlCatch);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(PearlCatchFeature feature, KeybindSetting preparedLowPearlCatch,
            BooleanSetting preparedAutoAim, KeybindSetting preparedInstantPearlCatch, FloatRangeSetting preparedHighDelay,
            BooleanSetting preparedSwitchBack, BooleanSetting preparedRetry, KeybindSetting preparedHighPearlCatch,
            BooleanSetting preparedSilentAim) {
        lowPearlCatch = preparedLowPearlCatch;
        autoAim = preparedAutoAim;
        instantPearlCatch = preparedInstantPearlCatch;
        highDelay = preparedHighDelay;
        switchBack = preparedSwitchBack;
        retry = preparedRetry;
        highPearlCatch = preparedHighPearlCatch;
        silentAim = preparedSilentAim;
    }
}
