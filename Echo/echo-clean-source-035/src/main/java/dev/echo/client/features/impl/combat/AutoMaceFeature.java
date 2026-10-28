package dev.echo.client.features.impl.combat;

import dev.echo.client.Echo;
import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.EventPriority;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.AttackKeyEvent;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.events.input.HotbarKeyEvent;
import dev.echo.client.events.input.MouseMovementEvent;
import dev.echo.client.events.input.UseKeyEvent;
import dev.echo.client.feature.combat.MaceFlightState;
import dev.echo.client.feature.combat.MaceImpactPrediction;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.handlers.AimTransition;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.handlers.RotationPriority;
import dev.echo.client.handlers.RotationSettings;
import dev.echo.client.handlers.RotationVisibility;
import dev.echo.client.handlers.ServerFallDistanceTracker;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.utils.ActionTimer;
import dev.echo.client.utils.interaction.ActionReadiness;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.simulation.FallSample;
import dev.echo.client.utils.simulation.MovementSimulation;
import dev.echo.client.utils.simulation.TickWindow;
import dev.echo.client.utils.strings.Concat;
import dev.echo.client.utils.targeting.AimPointMode;
import dev.echo.client.utils.targeting.AimPointSettings;
import dev.echo.client.utils.targeting.EntityRaycasts;
import dev.echo.client.utils.targeting.TargetSelection;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Mace attacks, aim requests and the timed chestplate/elytra exchange sequence. */
public class AutoMaceFeature extends Feature {
    private static final CharSequence BLATANT = CatalogText.literal("Blatant");
    private static final CharSequence LATE = CatalogText.literal("Late");
    private static final CharSequence CLOSEST = CatalogText.literal("Closest");
    private static final CharSequence NATURAL = CatalogText.literal("Natural");
    private static final CharSequence RANGE_SEPARATOR = CatalogText.literal("-");
    private static final int EQUIPMENT_USE_TICK = 2;
    private static final int EQUIPMENT_FINISH_TICK = 4;
    private static final int MINIMUM_PREDICTED_HIT_TICK = 3;
    private static final int AIM_PREDICTION_TICKS = 6;
    private static final int GLIDE_END_TIMEOUT_TICKS = 8;
    private static final double MINIMUM_SMASH_DISTANCE = 1.5;

    private static FloatSetting fallDistance;
    private static FloatRangeSetting cps;
    private static BooleanSetting workWithMaceSwap;
    private static BooleanSetting autoAim;
    private static ChoiceSetting aimMath;
    private static ChoiceSetting aimVector;
    private static ChoiceSetting targetPriority;
    private static BooleanSetting random;
    private static BooleanSetting silentRotations;
    private static BooleanSetting throughWalls;
    private static FloatSetting horizontalSpeed;
    private static FloatSetting verticalSpeed;
    private static FloatSetting range;
    private static FloatSetting yawFOV;
    private static FloatSetting pitchFOV;
    private static FloatSetting multipoint;
    private static BooleanSetting automaticallyUnequipElytra;
    private static ChoiceSetting unequipTiming;
    private static FloatSetting lateWindow;
    private static BooleanSetting reEquipElytra;

    private ActionTimer attackTimer;
    private long attackDelayMillis;
    private boolean disablePending;
    private int originalSlot;
    private int equipmentSlot;
    private int equipmentStageTicks;
    private boolean reEquippingElytra;
    private boolean equipmentUseIssued;
    private boolean chestplateSwapObserved;
    private ActionReadiness actionReadiness;
    private LocalPlayer trackedPlayer;
    private MaceFlightState flightState;
    private int glideEndWaitTicks;

    public AutoMaceFeature() {
        super(new FeatureInfo(CatalogText.literal("Auto Mace"),
                CatalogText.literal("Automatically attacks while holding a mace"), Category.COMBAT));
    }

    @Override
    public void postInitialize() {
        attackTimer = new ActionTimer();
        attackDelayMillis = 0L;
        disablePending = false;
        originalSlot = -1;
        equipmentSlot = -1;
        actionReadiness = new ActionReadiness();
        flightState = MaceFlightState.IDLE;
        configureSettings();
    }

    private void configureSettings() {
        workWithMaceSwap.description(CatalogText.literal("With the mace swap module enabled it will allow you to hold allowed items to automatically swap to a mace."));
        aimMath.setVisibilityPredicate(ignored -> autoAim.getValue());
        aimVector.setVisibilityPredicate(ignored -> autoAim.getValue());
        targetPriority.setVisibilityPredicate(ignored -> autoAim.getValue());
        random.setVisibilityPredicate(ignored -> autoAim.getValue());
        silentRotations.setVisibilityPredicate(ignored -> autoAim.getValue());
        throughWalls.setVisibilityPredicate(ignored -> autoAim.getValue());
        horizontalSpeed.setVisibilityPredicate(ignored -> autoAim.getValue() && !aimMath.isSelected(BLATANT));
        verticalSpeed.setVisibilityPredicate(ignored -> autoAim.getValue() && !aimMath.isSelected(BLATANT));
        range.setVisibilityPredicate(ignored -> autoAim.getValue() || automaticallyUnequipElytra.getValue());
        yawFOV.setVisibilityPredicate(ignored -> autoAim.getValue());
        pitchFOV.setVisibilityPredicate(ignored -> autoAim.getValue());
        multipoint.setVisibilityPredicate(ignored -> autoAim.getValue());
        cps.tooltip("Timing");
        horizontalSpeed.tooltip("Timing");
        verticalSpeed.tooltip("Timing");
        automaticallyUnequipElytra.description(CatalogText.literal("Equips a chestplate when the predicted fall can reach a mace target."));
        unequipTiming.description(CatalogText.literal("Normal starts at the first predicted opportunity. Late keeps gliding until a hit is inside the Late Window."));
        unequipTiming.setVisibilityPredicate(ignored -> automaticallyUnequipElytra.getValue());
        lateWindow.description(CatalogText.literal("Maximum ticks from starting the armor swap to the predicted hit. Lower swaps later; higher gives more preparation time. Includes the stun follow-up when needed."));
        lateWindow.setVisibilityPredicate(ignored -> automaticallyUnequipElytra.getValue() && unequipTiming.isSelected(LATE));
        reEquipElytra.description(CatalogText.literal("Puts the elytra back on after a smash bounce or when the dive ends on the ground, in liquid, or on a climbable block."));
        reEquipElytra.setVisibilityPredicate(ignored -> automaticallyUnequipElytra.getValue());
        automaticallyUnequipElytra.tooltip("Elytra");
        unequipTiming.tooltip("Elytra");
        lateWindow.tooltip("Elytra");
        reEquipElytra.tooltip("Elytra");
    }

    @Override
    public CharSequence getSuffix() {
        return Concat.sequences(new CharSequence[] {
                Concat.formattedFloat(cps.getLower(), 1), RANGE_SEPARATOR,
                Concat.formattedFloat(cps.getUpper(), 1)});
    }

    @Override
    public void onEnable() {
        disablePending = false;
        resetFlightState(true);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (client == null) {
            throw new NullPointerException("object reference is null");
        }
        if (client.player != null) {
            restoreEquipmentSlot();
        }
        resetFlightState(true);
        if (silentRotations.getValue() && rotations().isOwnedBy(this)) {
            disablePending = true;
            return;
        }
        disablePending = false;
        rotations().cancel(this);
        super.onDisable();
    }

    @Subscribe
    private void onMouseMovement(MouseMovementEvent.Post event) {
        if (disablePending) {
            finishDeferredDisable();
            return;
        }
        if (!autoAim.getValue()) {
            if (rotations().isOwnedBy(this)) {
                releaseOrCancelRotation();
            }
            return;
        }
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            rotations().beginReturn(this);
            return;
        }
        if (client.player.isFallFlying() || client.player.onGround()
                || !(client.player.getDeltaMovement().y < 0.0) || hasEquipmentSwap()) {
            releaseOrCancelRotation();
            return;
        }
        if (!canUseMace()) {
            if (silentRotations.getValue() && rotations().isOwnedBy(this)
                    && (MaceSwapFeature.hasPendingSwap() || SpearReachFeature.hasPendingSwap())) {
                return;
            }
            releaseOrCancelRotation();
            return;
        }
        boolean ready = isSmashReady();
        MaceImpactPrediction prediction = ready ? null : predictImpact(AIM_PREDICTION_TICKS);
        if (!ready && prediction == null) {
            releaseOrCancelRotation();
            return;
        }
        LivingEntity target = prediction == null ? null : TargetSelection.resolveTarget(prediction.target());
        if (target == null) {
            target = selectAimTarget();
        }
        if (target == null) {
            releaseOrCancelRotation();
            return;
        }
        boolean accepted;
        if (prediction != null && prediction.target() == target) {
            accepted = rotations().aim(this, RotationPriority.HIGH,
                    MovementSimulation.predictedAimPoint(target, prediction.impactTick()), rotationSettings());
        } else {
            accepted = rotations().aim(this, RotationPriority.HIGH, target, aimPointSettings(), rotationSettings());
        }
        if (!accepted) {
            releaseOrCancelRotation();
        }
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onHandleInput(HandleInputEvent.Pre event) {
        if (trackedPlayer != client.player) {
            resetFlightState(true);
            trackedPlayer = client.player;
        }
        if (disablePending || isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            return;
        }
        if (!client.player.isAlive()) {
            resetFlightState(true);
            return;
        }
        updateDive();
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onHotbarKey(HotbarKeyEvent event) {
        if (event.slot != 0 || disablePending || isOutsideWorld()
                || MinecraftRenderState.getScreen() != null) {
            return;
        }
        if (!workWithMaceSwap.getValue() || InventoryItems.mainHandStack().is(Items.MACE)
                || hasEquipmentSwap() || DeferredActionQueue.hasPendingActions()) {
            return;
        }
        if (client.player.isUsingItem() && InventoryItems.isSpear(client.player.getUseItem().getItem())) {
            return;
        }
        if (!isSmashReady() || !attackTimer.hasElapsedMillis((double) attackDelayMillis)) {
            return;
        }
        LivingEntity target = attackTarget();
        if (target == null) {
            return;
        }
        MaceSwapFeature swap = (MaceSwapFeature) Echo.featureManager.findFeature(MaceSwapFeature.class);
        if (swap == null || !swap.isEnabled() || !swap.requestMaceSwap(target)) {
            return;
        }
        resetAttackTimer();
    }

    @Subscribe
    private void onAttackKey(AttackKeyEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null
                || hasEquipmentSwap() || DeferredActionQueue.hasPendingActions() || !isSmashReady()) {
            return;
        }
        if (attackTarget() == null || !canUseMace() || !attackTimer.hasElapsedMillis((double) attackDelayMillis)) {
            return;
        }
        event.addClick();
        resetAttackTimer();
    }

    @Subscribe
    private void onUseKey(UseKeyEvent event) {
        if (!hasEquipmentSwap() || isOutsideWorld() || equipmentStageTicks != EQUIPMENT_USE_TICK
                || equipmentUseIssued || client.player.getInventory().getSelectedSlot() != equipmentSlot) {
            return;
        }
        boolean matches = reEquippingElytra ? InventoryItems.mainHandStack().is(Items.ELYTRA)
                : InventoryItems.holdsChestplateInMainHand();
        if (!matches) {
            return;
        }
        if (!event.hasClicks()) {
            event.addClick();
        }
        equipmentUseIssued = true;
    }

    private void updateDive() {
        if (!automaticallyUnequipElytra.getValue()) {
            restoreEquipmentSlot();
            resetFlightState(true);
            return;
        }
        updateReequipReadiness();
        if (hasEquipmentSwap(false)) {
            updateUnequip();
            return;
        }
        if (hasEquipmentSwap(true)) {
            updateReequip();
            return;
        }
        if (!client.player.isFallFlying()) {
            tryReequip();
            resetFlightState(false);
            return;
        }
        if (flightState == MaceFlightState.WAITING_FOR_GLIDE_END) {
            if (++glideEndWaitTicks > GLIDE_END_TIMEOUT_TICKS) {
                flightState = MaceFlightState.ABORTED_THIS_FLIGHT;
            }
            return;
        }
        if (flightState == MaceFlightState.ABORTED_THIS_FLIGHT) {
            return;
        }
        if (DeferredActionQueue.hasPendingActions() || client.player.isUsingItem()
                || client.player.isInWater() || client.player.isInLava()) {
            return;
        }
        if (InventoryItems.findMaceWithEnchantmentInHotbar(null, true) == -1) {
            return;
        }
        int chestplate = InventoryItems.findChestplateInHotbar();
        if (chestplate == -1 || !shouldStartUnequip()) {
            return;
        }
        startEquipmentSwap(chestplate, false);
        if (!hasEquipmentSwap()) {
            flightState = MaceFlightState.ABORTED_THIS_FLIGHT;
        }
    }

    private void updateUnequip() {
        if (client.player.getInventory().getSelectedSlot() != equipmentSlot) {
            boolean observed = observeChestplateSwap();
            clearEquipmentSwapState();
            finishUnequip(observed);
            return;
        }
        observeChestplateSwap();
        if (!client.player.isFallFlying() && !chestplateSwapObserved && equipmentStageTicks < EQUIPMENT_USE_TICK) {
            restoreEquipmentSlot();
            resetFlightState(false);
            return;
        }
        if (++equipmentStageTicks < EQUIPMENT_FINISH_TICK) {
            return;
        }
        boolean observed = chestplateSwapObserved;
        restoreEquipmentSlot();
        finishUnequip(observed);
    }

    private boolean observeChestplateSwap() {
        if (chestplateSwapObserved || equipmentSlot < 0) {
            return chestplateSwapObserved;
        }
        chestplateSwapObserved = !InventoryItems.wearsElytra()
                && client.player.getInventory().getItem(equipmentSlot).is(Items.ELYTRA);
        if (chestplateSwapObserved && reEquipElytra.getValue()) {
            actionReadiness.beginWaiting();
            updateReequipReadiness();
        }
        return chestplateSwapObserved;
    }

    private void finishUnequip(boolean observed) {
        flightState = observed ? MaceFlightState.WAITING_FOR_GLIDE_END : MaceFlightState.ABORTED_THIS_FLIGHT;
        glideEndWaitTicks = 0;
        if (!observed) {
            actionReadiness.reset();
        }
        if (!client.player.isFallFlying()) {
            resetFlightState(false);
        }
    }

    private void updateReequip() {
        if (client.player.getInventory().getSelectedSlot() != equipmentSlot) {
            clearEquipmentSwapState();
            actionReadiness.reset();
            return;
        }
        if (++equipmentStageTicks < EQUIPMENT_FINISH_TICK) {
            return;
        }
        restoreEquipmentSlot();
        actionReadiness.reset();
    }

    private void updateReequipReadiness() {
        if (!reEquipElytra.getValue() || InventoryItems.wearsElytra()) {
            actionReadiness.reset();
            return;
        }
        if (client.player.isFallFlying()) {
            return;
        }
        boolean supported = client.player.onGround() || client.player.isInWater()
                || client.player.isInLava() || client.player.onClimbable();
        actionReadiness.update(client.player.getY() - client.player.yo, supported);
    }

    private void tryReequip() {
        if (!actionReadiness.isReady() || DeferredActionQueue.hasPendingActions() || client.player.isUsingItem()) {
            return;
        }
        int elytra = InventoryItems.findStackInHotbar(stack -> stack.is(Items.ELYTRA));
        if (elytra == -1) {
            actionReadiness.reset();
            return;
        }
        startEquipmentSwap(elytra, true);
    }

    private void startEquipmentSwap(int slot, boolean reEquip) {
        if (hasEquipmentSwap() || DeferredActionQueue.hasPendingActions()) {
            return;
        }
        int currentSlot = client.player.getInventory().getSelectedSlot();
        if (currentSlot != slot && !DeferredActionQueue.select(this, slot, -1, false)) {
            return;
        }
        originalSlot = currentSlot;
        equipmentSlot = slot;
        equipmentStageTicks = 0;
        reEquippingElytra = reEquip;
        equipmentUseIssued = false;
        chestplateSwapObserved = false;
    }

    private boolean shouldStartUnequip() {
        if (unequipTiming.isSelected(LATE)) {
            return shouldStartLateUnequip();
        }
        List<FallSample> immediate = MovementSimulation.fallSamples(0);
        List<FallSample> delayed = MovementSimulation.fallSamples(1);
        if (immediate.size() <= 1 || delayed.size() <= 1) {
            return false;
        }
        AABB bounds = MovementSimulation.sampleBounds(immediate).minmax(MovementSimulation.sampleBounds(delayed));
        double minimumFall = Math.max(MINIMUM_SMASH_DISTANCE, fallDistance.getValue());
        double targetRange = range.getValue();
        for (LivingEntity target : predictionTargets(bounds)) {
            if (!MovementSimulation.isHorizontallyReachable(immediate, target, targetRange)
                    && !MovementSimulation.isHorizontallyReachable(delayed, target, targetRange)) {
                continue;
            }
            if (MovementSimulation.firstImpactTick(immediate, target, minimumFall) >= MINIMUM_PREDICTED_HIT_TICK
                    && MovementSimulation.firstImpactTick(delayed, target, minimumFall) >= MINIMUM_PREDICTED_HIT_TICK) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldStartLateUnequip() {
        List<FallSample> first = MovementSimulation.fallSamples(2);
        List<FallSample> second = MovementSimulation.fallSamples(3);
        if (first.size() <= 1 || second.size() <= 1) {
            return false;
        }
        AABB bounds = MovementSimulation.sampleBounds(first).minmax(MovementSimulation.sampleBounds(second));
        double minimumFall = Math.max(MINIMUM_SMASH_DISTANCE, fallDistance.getValue());
        int lastTick = Math.round(lateWindow.getValue());
        MaceSwapFeature swap = (MaceSwapFeature) Echo.featureManager.findFeature(MaceSwapFeature.class);
        for (LivingEntity target : predictionTargets(bounds)) {
            int followupTicks = swap == null ? 0 : swap.getStunDelayTicks(target);
            TickWindow window = new TickWindow(EQUIPMENT_FINISH_TICK, lastTick, followupTicks);
            if (MovementSimulation.canHitWithinWindow(first, target, minimumFall, range.getValue(), window)
                    && MovementSimulation.canHitWithinWindow(second, target, minimumFall, range.getValue(), window)) {
                return true;
            }
        }
        return false;
    }

    private MaceImpactPrediction predictImpact(int latestTick) {
        List<FallSample> samples = MovementSimulation.fallSamples(0);
        if (samples.size() <= 1) {
            return null;
        }
        double minimumFall = fallDistance.getValue();
        MaceImpactPrediction best = null;
        for (LivingEntity target : predictionTargets(MovementSimulation.sampleBounds(samples))) {
            int tick = MovementSimulation.firstImpactTick(samples, target, minimumFall);
            if (tick < 0 || tick > latestTick) {
                continue;
            }
            if (best == null || tick < best.impactTick()) {
                best = new MaceImpactPrediction(target, tick);
            }
        }
        return best;
    }

    private List<LivingEntity> predictionTargets(AABB bounds) {
        return client.level.getEntitiesOfClass(LivingEntity.class,
                bounds.inflate(MovementSimulation.attackRange() + 1.0), this::isPredictionTarget);
    }

    private boolean isPredictionTarget(LivingEntity entity) {
        return entity != client.player && entity.isAlive() && !entity.isRemoved() && !entity.isInvulnerable()
                && TargetSelection.isValidTarget(entity) && entity.getY() <= client.player.getY() + 1.0;
    }

    private boolean hasEquipmentSwap() {
        return equipmentSlot >= 0;
    }

    private boolean hasEquipmentSwap(boolean reEquip) {
        return hasEquipmentSwap() && reEquippingElytra == reEquip;
    }

    private void restoreEquipmentSlot() {
        if (!hasEquipmentSwap()) {
            return;
        }
        if (originalSlot >= 0 && client.player.getInventory().getSelectedSlot() == equipmentSlot) {
            InventoryItems.selectHotbarSlot(originalSlot);
        }
        clearEquipmentSwapState();
    }

    private void clearEquipmentSwapState() {
        DeferredActionQueue.finish(this, false);
        originalSlot = -1;
        equipmentSlot = -1;
        equipmentStageTicks = 0;
        reEquippingElytra = false;
        equipmentUseIssued = false;
        chestplateSwapObserved = false;
    }

    private void resetFlightState(boolean clearEquipmentState) {
        flightState = MaceFlightState.IDLE;
        glideEndWaitTicks = 0;
        chestplateSwapObserved = false;
        if (!clearEquipmentState) {
            return;
        }
        actionReadiness.reset();
        clearEquipmentSwapState();
    }

    private LivingEntity selectAimTarget() {
        double targetRange = range.getValue();
        double rangeSquared = targetRange * targetRange;
        Vec3 eye = client.player.getEyePosition(1.0F);
        List<LivingEntity> candidates = client.level.getEntitiesOfClass(LivingEntity.class,
                client.player.getBoundingBox().inflate(targetRange),
                entity -> isAimCandidate(eye, rangeSquared, entity));
        LivingEntity target = TargetSelection.resolveTarget(
                EntityRaycasts.selectTarget(client.player, candidates, eye, targetPriority));
        if (target == null || eye.distanceToSqr(target.position()) > rangeSquared) {
            return null;
        }
        return isWithinFov(target) ? target : null;
    }

    private AimPointSettings aimPointSettings() {
        return new AimPointSettings(aimVector.isSelected(CLOSEST) ? AimPointMode.CLOSEST : AimPointMode.STRAIGHT,
                multipoint.getValue(), random.getValue(), throughWalls.getValue());
    }

    private RotationSettings rotationSettings() {
        return new RotationSettings(silentRotations.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE,
                aimTransition(), horizontalSpeed.getValue() * 20.0F, verticalSpeed.getValue() * 20.0F);
    }

    private LivingEntity attackTarget() {
        LivingEntity target = crosshairLivingTarget();
        if (target == null || !TargetSelection.isValidTarget(target)) {
            return null;
        }
        LivingEntity locked = TargetSelection.resolveTarget(target);
        if (locked == null || locked != target || target.isInvulnerable() || target.isDeadOrDying() || !target.isAlive()) {
            return null;
        }
        return target;
    }

    private void resetAttackTimer() {
        float sampledCps = cps.sampleGaussian();
        attackDelayMillis = sampledCps <= 0.0F ? 0L : (long) (1000.0F / sampledCps);
        attackTimer.reset();
    }

    private boolean isSmashReady() {
        return !client.player.isFallFlying() && ServerFallDistanceTracker.fallDistance(client.player) > fallDistance.getValue();
    }

    private LivingEntity crosshairLivingTarget() {
        if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.ENTITY) {
            return null;
        }
        if (!(((EntityHitResult) client.hitResult).getEntity() instanceof LivingEntity target)) {
            return null;
        }
        return !target.isAlive() || target.isRemoved() ? null : target;
    }

    private boolean canUseMace() {
        ItemStack held = InventoryItems.mainHandStack();
        if (held.is(Items.MACE)) {
            return true;
        }
        if (!workWithMaceSwap.getValue()) {
            return false;
        }
        MaceSwapFeature swap = (MaceSwapFeature) Echo.featureManager.findFeature(MaceSwapFeature.class);
        if (swap == null || !swap.isEnabled()) {
            return false;
        }
        if (!MaceSwapFeature.itemWhitelist.acceptsHeldItem() || !MaceSwapFeature.hotbarWhitelist.permitsCurrentSlot()) {
            return false;
        }
        return InventoryItems.findMaceWithEnchantmentInHotbar(null, true) != -1;
    }

    private void releaseOrCancelRotation() {
        if (silentRotations.getValue()) {
            rotations().beginReturn(this);
        } else {
            rotations().cancel(this);
        }
    }

    private void finishDeferredDisable() {
        if (rotations().beginReturn(this)) {
            return;
        }
        disablePending = false;
        super.onDisable();
    }

    private AimTransition aimTransition() {
        if (aimMath.isSelected(NATURAL)) {
            return AimTransition.NATURAL;
        }
        return aimMath.isSelected(BLATANT) ? AimTransition.SNAP : AimTransition.EASED;
    }

    private boolean isWithinFov(LivingEntity entity) {
        return EntityRaycasts.isWithinFov(client.player, entity, yawFOV.getValue(), pitchFOV.getValue());
    }

    private boolean isAimCandidate(Vec3 eye, double rangeSquared, LivingEntity entity) {
        return entity != client.player && !entity.isDeadOrDying() && entity.isAlive() && TargetSelection.isValidTarget(entity)
                && eye.distanceToSqr(entity.position()) <= rangeSquared && isWithinFov(entity);
    }

    /** Classfile declaration order, distinct from the metadata assignment order. */
    public static void appendBootstrapSettings(AutoMaceFeature feature) {
        CatalogSettings.append(feature, fallDistance);
        CatalogSettings.append(feature, cps);
        CatalogSettings.append(feature, workWithMaceSwap);
        CatalogSettings.append(feature, autoAim);
        CatalogSettings.append(feature, aimMath);
        CatalogSettings.append(feature, aimVector);
        CatalogSettings.append(feature, targetPriority);
        CatalogSettings.append(feature, random);
        CatalogSettings.append(feature, silentRotations);
        CatalogSettings.append(feature, throughWalls);
        CatalogSettings.append(feature, horizontalSpeed);
        CatalogSettings.append(feature, verticalSpeed);
        CatalogSettings.append(feature, range);
        CatalogSettings.append(feature, yawFOV);
        CatalogSettings.append(feature, pitchFOV);
        CatalogSettings.append(feature, multipoint);
        CatalogSettings.append(feature, automaticallyUnequipElytra);
        CatalogSettings.append(feature, unequipTiming);
        CatalogSettings.append(feature, lateWindow);
        CatalogSettings.append(feature, reEquipElytra);
    }

    /** Publishes only the already-constructed settings, in metadata vector order. */
    public static void assignBootstrapSettings(AutoMaceFeature feature,
            FloatRangeSetting preparedCps, BooleanSetting preparedWorkWithMaceSwap,
            FloatSetting preparedMultipoint, FloatSetting preparedPitchFOV, ChoiceSetting preparedTargetPriority,
            FloatSetting preparedRange, BooleanSetting preparedReEquipElytra, BooleanSetting preparedSilentRotations,
            FloatSetting preparedFallDistance, BooleanSetting preparedAutomaticallyUnequipElytra,
            BooleanSetting preparedAutoAim, BooleanSetting preparedRandom, FloatSetting preparedVerticalSpeed,
            BooleanSetting preparedThroughWalls, FloatSetting preparedYawFOV, ChoiceSetting preparedUnequipTiming,
            FloatSetting preparedHorizontalSpeed, FloatSetting preparedLateWindow,
            ChoiceSetting preparedAimMath, ChoiceSetting preparedAimVector) {
        cps = preparedCps;
        workWithMaceSwap = preparedWorkWithMaceSwap;
        multipoint = preparedMultipoint;
        pitchFOV = preparedPitchFOV;
        targetPriority = preparedTargetPriority;
        range = preparedRange;
        reEquipElytra = preparedReEquipElytra;
        silentRotations = preparedSilentRotations;
        fallDistance = preparedFallDistance;
        automaticallyUnequipElytra = preparedAutomaticallyUnequipElytra;
        autoAim = preparedAutoAim;
        random = preparedRandom;
        verticalSpeed = preparedVerticalSpeed;
        throughWalls = preparedThroughWalls;
        yawFOV = preparedYawFOV;
        unequipTiming = preparedUnequipTiming;
        horizontalSpeed = preparedHorizontalSpeed;
        lateWindow = preparedLateWindow;
        aimMath = preparedAimMath;
        aimVector = preparedAimVector;
    }
}
