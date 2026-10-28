package dev.echo.client.features.impl.utility;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.config.FeatureNameAssignments;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.MouseMovementEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.network.PacketSendEvent;
import dev.echo.client.feature.utility.PearlGrapplePhase;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.handlers.AimTransition;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.handlers.EntityPositionTracker;
import dev.echo.client.handlers.LatencyTracker;
import dev.echo.client.handlers.PositionSample;
import dev.echo.client.utils.Rotation;
import dev.echo.client.handlers.RotationPriority;
import dev.echo.client.handlers.RotationManager;
import dev.echo.client.handlers.RotationSettings;
import dev.echo.client.handlers.RotationVisibility;
import dev.echo.client.handlers.VelocitySample;
import dev.echo.client.mixin.MinecraftClientAccessor;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.targeting.CrosshairHits;
import dev.echo.client.utils.targeting.TargetSelection;
import dev.echo.client.utils.trajectory.MovingTargetLaunch;
import dev.echo.client.utils.trajectory.MovingTargetTrajectorySearch;
import dev.echo.client.utils.trajectory.TargetPath;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Selects a moving player, waits for a reliable launch, then restores the held slot. */
public class PearlGrappleFeature extends Feature {
    private static final double EYE_OFFSET = 0.1;
    private static final float MAX_TARGET_DISTANCE = 48.0f;
    private static final double REQUIRED_HIT_FRACTION = 0.5;
    private static final double MANUAL_HIT_TOLERANCE = 0.15;
    private static final int AIM_WAIT_TICKS = 10;
    private static final double AIM_POINT_DISTANCE = 100.0;
    private static final float MIN_ROTATION_SPEED = 180.0f;
    private static final float MAX_ROTATION_SPEED = 1200.0f;
    private static final double TELEPORT_DISTANCE = 8.0;
    private static final int TELEPORT_SAMPLE_TICKS = 5;
    private static final double FAST_TARGET_SPEED = 0.8;
    private static final int WIND_CHARGE_WAIT_TICKS = 20;
    private static final float WIND_CHARGE_PITCH = 50.0f;
    private static final double VELOCITY_PACKET_THRESHOLD_SQUARED = 0.1;
    private static final double RISE_VELOCITY = 0.6;
    private static final int REQUIRED_RISE_SAMPLES = 3;
    private static final double HIGH_LATENCY_SPEED = 0.5;
    private static final int HIGH_LATENCY_MILLIS = 150;
    private static final double ORDINARY_MOVING_HIT_FRACTION = 0.5;
    private static final double PERTURBED_HIT_FRACTION = 0.25;
    private static final double PREDICTION_PERTURBATION_TICKS = 1.0;
    private static final double FAST_FALL_VELOCITY = -1.0;
    private static final double FALLING_HIT_FRACTION = 0.9;
    private static final CharSequence NO_PEARL = CatalogText.literal("No ender pearl in hotbar");
    private static final CharSequence PEARL_COOLDOWN = CatalogText.literal("Ender pearl is on cooldown");
    private static final CharSequence NO_TARGET = CatalogText.literal("No target in range");
    private static final CharSequence TARGET_LEFT_RANGE = CatalogText.literal("Target left range");
    private static final CharSequence BLOCKED_PATH = CatalogText.literal("Pearl path is blocked");
    private static final CharSequence NO_RELIABLE_SHOT = CatalogText.literal("No reliable shot");
    private static FloatSetting distance;
    private static IntSetting maxPredictTicks;
    private static BooleanSetting silentRotations;
    private PearlGrapplePhase phase;
    private Player target;
    private int selectedAtPlayerTick;
    private int selectedAtTrackerTick;
    private boolean delayedForWindCharge;
    private int riseStartTrackerTick;
    private TargetPath targetPath;
    private MovingTargetTrajectorySearch.Result targetMotion;
    private MovingTargetLaunch bestLaunch;
    private int pathUpdatedPlayerTick;
    private Vec3 lastSentPosition;
    private Vec3 sentVelocity;
    private boolean sentOnGround;
    private boolean sentMovementThisTick;

    public PearlGrappleFeature() {
        super(new FeatureInfo(CatalogText.literal("Pearl Grapple"),
            CatalogText.literal("Throws an ender pearl that hits the best player target"), Category.UTILITY));
    }

    @Override public boolean isFilteredFromDefaultList() { return true; }

    @Override
    public void postInitialize() {
        phase = PearlGrapplePhase.SELECT;
        riseStartTrackerTick = -1;
        pathUpdatedPlayerTick = -1;
        sentVelocity = Vec3.ZERO;
        sentOnGround = true;
        maxPredictTicks.description(CatalogText.literal("How many ticks of pearl flight and target movement are simulated"));
    }

    @Override
    public void onEnable() {
        // NB-097: the original PearlCatch guard's true branch is this candidate's domain.
        try {
            Class.forName(PearlCatchFeature.class.getName(), true, PearlCatchFeature.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        PearlGrapplePhase selected = PearlGrapplePhase.SELECT;
        try {
            Class.forName(PearlGrapplePhase.class.getName(), true, PearlGrapplePhase.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        phase = selected;
        initializePearlPlayerType();
        target = null;
        targetPath = null;
        targetMotion = null;
        bestLaunch = null;
        pathUpdatedPlayerTick = -1;
        initializePearlClientType();
        Vec3 position;
        if (client.player != null) {
            initializePearlClientType();
            position = client.player.position();
        } else position = null;
        initializePearlVectorType();
        lastSentPosition = position;
        initializePearlClientType();
        Vec3 movement;
        if (client.player != null) {
            initializePearlClientType();
            Vec3 current = client.player.position();
            initializePearlClientType();
            double previousX = client.player.xo;
            initializePearlClientType();
            double previousY = client.player.yo;
            initializePearlClientType();
            double previousZ = client.player.zo;
            movement = current.subtract(previousX, previousY, previousZ);
        } else movement = Vec3.ZERO;
        initializePearlVectorType();
        sentVelocity = movement;
        initializePearlClientType();
        boolean onGround;
        if (client.player != null) {
            initializePearlClientType();
            onGround = client.player.onGround();
        } else onGround = true;
        sentOnGround = onGround;
        sentMovementThisTick = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        DeferredActionQueue.finish(this, true);
        initializePearlRotationType();
        RotationManager rotations = rotations();
        initializePearlRotationType();
        rotations.cancel(this);
        initializePearlPlayerType();
        target = null;
        targetPath = null;
        targetMotion = null;
        bestLaunch = null;
        super.onDisable();
    }

    @Subscribe
    private void onTick(ClientTickEvent event) {
        if (isOutsideWorld()) {
            setEnabled(false);
            return;
        }
        if (MinecraftRenderState.getScreen() != null) return;
        switch (phase) {
            case SELECT -> select();
            case AIM -> aim();
            case RESTORE -> setEnabled(false);
        }
    }

    private void select() {
        int slot = InventoryItems.findStackInHotbar(stack -> stack.is(Items.ENDER_PEARL));
        if (slot == -1) {
            fail(NO_PEARL);
            return;
        }
        if (isPearlCoolingDown()) {
            fail(PEARL_COOLDOWN);
            return;
        }
        target = selectTarget();
        if (target == null) {
            fail(NO_TARGET);
            return;
        }
        if (client.player.getInventory().getSelectedSlot() != slot
                && !DeferredActionQueue.ensureSelected(this, slot, -1, true)) {
            setEnabled(false);
            return;
        }
        selectedAtPlayerTick = client.player.tickCount;
        selectedAtTrackerTick = EntityPositionTracker.currentTick();
        delayedForWindCharge = false;
        riseStartTrackerTick = -1;
        phase = PearlGrapplePhase.AIM;
    }

    private void aim() {
        int tick = client.player.tickCount;
        if (!client.player.getMainHandItem().is(Items.ENDER_PEARL)) {
            fail(NO_PEARL);
            return;
        }
        if (!isValidTarget(target)) {
            fail(TARGET_LEFT_RANGE);
            return;
        }
        if (tick <= selectedAtPlayerTick || pathUpdatedPlayerTick != tick) return;
        if (tick - selectedAtPlayerTick < WIND_CHARGE_WAIT_TICKS && shouldWaitForWindCharge(target)) {
            delayedForWindCharge = true;
            return;
        }
        VelocitySample velocity = EntityPositionTracker.velocity(target);
        if (velocity != null && targetMotion != null && targetMotion.tick() < velocity.tick()) return;
        boolean receivedVelocity = velocity != null && velocity.tick() >= selectedAtTrackerTick;
        if (!receivedVelocity && tick - selectedAtPlayerTick < 30 && awaitingRiseSamples()) return;
        boolean force = tick - selectedAtPlayerTick >= AIM_WAIT_TICKS + (delayedForWindCharge ? WIND_CHARGE_WAIT_TICKS : 0);
        MovingTargetLaunch launch = acceptableLaunch(force);
        if (launch != null) throwPearl(launch);
        if (phase == PearlGrapplePhase.AIM && force) {
            fail(targetPath != null && MovingTargetTrajectorySearch.findLaunch(targetPath.withoutObstructions()) != null
                && bestLaunch == null ? BLOCKED_PATH : NO_RELIABLE_SHOT);
        }
    }

    private MovingTargetLaunch acceptableLaunch(boolean force) {
        if (bestLaunch == null || (!targetMotion.reliable() && !force)) return null;
        MovingTargetLaunch launch = bestLaunch;
        if (!silentRotations.getValue()) {
            float yaw = client.player.getYRot();
            float pitch = client.player.getXRot();
            double fraction = MovingTargetTrajectorySearch.hitFraction(targetPath, yaw, pitch);
            double required = force ? 0.0 : Math.min(REQUIRED_HIT_FRACTION, bestLaunch.hitFraction() - MANUAL_HIT_TOLERANCE);
            if (fraction <= 0.0 || fraction < required) return null;
            launch = new MovingTargetLaunch(yaw, pitch, fraction, bestLaunch.meanHitTick());
        }
        if (requiresRobustPrediction()) return remainsReliableWhenPerturbed(launch) ? launch : null;
        if (!(launch.hitFraction() >= REQUIRED_HIT_FRACTION) && !force) return null;
        return launch;
    }

    private boolean awaitingRiseSamples() {
        if (riseStartTrackerTick < 0) return false;
        return EntityPositionTracker.positions(target).stream()
            .filter(sample -> sample.tick() >= riseStartTrackerTick).count() < REQUIRED_RISE_SAMPLES;
    }

    private boolean shouldWaitForWindCharge(Player player) {
        VelocitySample velocity = EntityPositionTracker.velocity(player);
        if (velocity != null && velocity.tick() >= selectedAtTrackerTick
                && velocity.velocity().lengthSqr() > VELOCITY_PACKET_THRESHOLD_SQUARED) return false;
        if (targetMotion != null && targetMotion.velocity().y > RISE_VELOCITY) {
            if (riseStartTrackerTick < 0) riseStartTrackerTick = EntityPositionTracker.currentTick();
            return false;
        }
        boolean holdsWindCharge = player.getMainHandItem().is(Items.WIND_CHARGE)
            || player.getOffhandItem().is(Items.WIND_CHARGE);
        return holdsWindCharge && player.getXRot() >= WIND_CHARGE_PITCH;
    }

    private boolean requiresRobustPrediction() {
        double speed = LatencyTracker.milliseconds() > HIGH_LATENCY_MILLIS ? HIGH_LATENCY_SPEED : FAST_TARGET_SPEED;
        if (targetMotion.velocity().length() > speed) return true;
        Vec3 velocity = targetMotion.velocity();
        return sentVelocity.y < FAST_FALL_VELOCITY && !sentOnGround && velocity.horizontalDistance() > Math.abs(velocity.y);
    }

    private boolean remainsReliableWhenPerturbed(MovingTargetLaunch launch) {
        boolean falling = sentVelocity.y < FAST_FALL_VELOCITY && !sentOnGround;
        if (launch.hitFraction() < (falling ? FALLING_HIT_FRACTION : ORDINARY_MOVING_HIT_FRACTION)) return false;
        double age = predictionAge(targetMotion);
        int ticks = maxPredictTicks.getValue();
        for (double offset : new double[] {-PREDICTION_PERTURBATION_TICKS, PREDICTION_PERTURBATION_TICKS}) {
            AABB[] boxes = MovingTargetTrajectorySearch.predictBoxes(target, targetMotion, age + offset, ticks);
            TargetPath perturbed = targetPath.withTargetBoxes(boxes);
            if (MovingTargetTrajectorySearch.hitFraction(perturbed, launch.yaw(), launch.pitch()) < PERTURBED_HIT_FRACTION) return false;
        }
        return true;
    }

    private void throwPearl(MovingTargetLaunch launch) {
        if (isPearlCoolingDown()) {
            fail(PEARL_COOLDOWN);
            return;
        }
        if (silentRotations.getValue() && rotations().override(this, RotationPriority.HIGH,
                new Rotation(launch.yaw(), launch.pitch())).map(scope -> {
                    scope.commit();
                    return true;
                }).isEmpty()) return;
        CrosshairHits.withItemUseHit(() -> ((MinecraftClientAccessor) client).invokeDoItemUse());
        rotations().cancel(this);
        phase = PearlGrapplePhase.RESTORE;
        if (!isPearlCoolingDown()) setEnabled(false);
    }

    private void fail(CharSequence message) {
        FeatureNameAssignments.postNotification(this, message);
        setEnabled(false);
    }

    private boolean isPearlCoolingDown() {
        return client.player.getCooldowns().isOnCooldown(new ItemStack(Items.ENDER_PEARL));
    }

    @Subscribe
    private void onMouseMovement(MouseMovementEvent.Post event) {
        if (isOutsideWorld() || silentRotations.getValue() || phase != PearlGrapplePhase.AIM) return;
        if (bestLaunch == null) {
            rotations().cancel(this);
            return;
        }
        Vec3 point = client.player.getEyePosition().add(Vec3.directionFromRotation(bestLaunch.pitch(), bestLaunch.yaw()).scale(AIM_POINT_DISTANCE));
        float yawDelta = Math.abs(Mth.wrapDegrees(bestLaunch.yaw() - client.player.getYRot()));
        float pitchDelta = Math.abs(bestLaunch.pitch() - client.player.getXRot());
        float speed = Mth.clamp(Math.max(yawDelta, pitchDelta) / 0.1f, MIN_ROTATION_SPEED, MAX_ROTATION_SPEED);
        rotations().aim(this, RotationPriority.HIGH, point,
            RotationSettings.uniform(RotationVisibility.VISIBLE, AimTransition.NATURAL, speed));
    }

    @Subscribe
    private void onPacketSend(PacketSendEvent event) {
        if (client.player == null) return;
        if (event.getPacket() instanceof ServerboundMovePlayerPacket movement) {
            if (movement.hasPosition()) {
                Vec3 position = new Vec3(movement.getX(0.0), movement.getY(0.0), movement.getZ(0.0));
                sentVelocity = lastSentPosition == null ? client.player.getDeltaMovement() : position.subtract(lastSentPosition);
                lastSentPosition = position;
            } else {
                sentVelocity = Vec3.ZERO;
            }
            sentOnGround = movement.isOnGround();
            sentMovementThisTick = true;
        } else if (event.getPacket() instanceof ServerboundClientTickEndPacket) {
            if (!sentMovementThisTick) sentVelocity = Vec3.ZERO;
            sentMovementThisTick = false;
            if (phase == PearlGrapplePhase.AIM && isValidTarget(target)) {
                targetMotion = estimateMotion(target);
                targetPath = buildTargetPath(target, targetMotion);
                bestLaunch = MovingTargetTrajectorySearch.findLaunch(targetPath);
                pathUpdatedPlayerTick = client.player.tickCount;
            }
        } else if (event.getPacket() instanceof ServerboundAcceptTeleportationPacket) {
            lastSentPosition = null;
            sentVelocity = Vec3.ZERO;
        }
    }

    private TargetPath buildTargetPath(Player player, MovingTargetTrajectorySearch.Result motion) {
        Vec3 origin = lastSentPosition != null ? lastSentPosition : client.player.position();
        Vec3 eye = origin.add(0.0, client.player.getEyeHeight() - EYE_OFFSET, 0.0);
        Vec3 inherited = new Vec3(sentVelocity.x, sentOnGround ? 0.0 : sentVelocity.y, sentVelocity.z);
        int ticks = maxPredictTicks.getValue();
        double age = predictionAge(motion);
        AABB[] boxes = MovingTargetTrajectorySearch.predictBoxes(player, motion, age, ticks);
        return new TargetPath(eye, inherited, boxes, blockers(player));
    }

    private static MovingTargetTrajectorySearch.Result estimateMotion(Player player) {
        int tick = EntityPositionTracker.currentTick();
        VelocitySample velocity = EntityPositionTracker.velocity(player);
        List<PositionSample> history = EntityPositionTracker.positions(player);
        return MovingTargetTrajectorySearch.estimateMotion(player, history, velocity, tick);
    }

    private static double predictionAge(MovingTargetTrajectorySearch.Result motion) {
        return (double) (EntityPositionTracker.currentTick() + 1 - motion.tick()) + (double) LatencyTracker.milliseconds() / 50.0;
    }

    private List<AABB> blockers(Player player) {
        return client.level.getEntities(client.player, client.player.getBoundingBox().inflate(targetDistance()),
            entity -> entity != player && entity.canBeHitByProjectile() && !(entity instanceof Projectile))
            .stream().map(Entity::getBoundingBox).toList();
    }

    private Player selectTarget() {
        Player best = null;
        double bestFraction = -1.0;
        double bestDistance = Double.MAX_VALUE;
        for (Player player : client.level.players()) {
            if (!isValidTarget(player)) continue;
            double playerDistance = client.player.distanceTo(player);
            MovingTargetTrajectorySearch.Result motion = estimateMotion(player);
            TargetPath path = buildTargetPath(player, motion);
            MovingTargetLaunch launch = MovingTargetTrajectorySearch.findLaunch(path);
            double fraction = launch == null ? 0.0 : launch.hitFraction();
            if (fraction > bestFraction || (fraction == bestFraction && playerDistance < bestDistance)) {
                best = player;
                bestFraction = fraction;
                bestDistance = playerDistance;
            }
        }
        return best;
    }

    private boolean isValidTarget(Player player) {
        return player != null && player != client.player && TargetSelection.isValidTarget(player)
            && client.player.distanceTo(player) <= targetDistance() && !player.isPassenger()
            && !player.isAutoSpinAttack() && !recentlyTeleported(player);
    }

    private static boolean recentlyTeleported(Player player) {
        List<PositionSample> history = EntityPositionTracker.positions(player);
        for (int i = 1; i < history.size(); i++) {
            if (EntityPositionTracker.currentTick() - history.get(i).tick() > TELEPORT_SAMPLE_TICKS) continue;
            if (history.get(i).position().distanceTo(history.get(i - 1).position()) > TELEPORT_DISTANCE) return true;
        }
        return false;
    }

    private static float targetDistance() { return Math.min(distance.getValue(), MAX_TARGET_DISTANCE); }

    private static void initializePearlPlayerType() {
        try { Class.forName(Player.class.getName(), true, Player.class.getClassLoader()); }
        catch (ClassNotFoundException failure) { throw new IllegalStateException("Resolved class initialization failed", failure); }
    }

    private static void initializePearlClientType() {
        try { Class.forName(Minecraft.class.getName(), true, Minecraft.class.getClassLoader()); }
        catch (ClassNotFoundException failure) { throw new IllegalStateException("Resolved class initialization failed", failure); }
    }

    private static void initializePearlVectorType() {
        try { Class.forName(Vec3.class.getName(), true, Vec3.class.getClassLoader()); }
        catch (ClassNotFoundException failure) { throw new IllegalStateException("Resolved class initialization failed", failure); }
    }

    private static void initializePearlRotationType() {
        try { Class.forName(RotationManager.class.getName(), true, RotationManager.class.getClassLoader()); }
        catch (ClassNotFoundException failure) { throw new IllegalStateException("Resolved class initialization failed", failure); }
    }

    public static void appendBootstrapSettings(PearlGrappleFeature feature) {
        CatalogSettings.append(feature, distance);
        CatalogSettings.append(feature, maxPredictTicks);
        CatalogSettings.append(feature, silentRotations);
    }

    public static void assignBootstrapSettings(PearlGrappleFeature feature, IntSetting preparedMaxPredictTicks,
            FloatSetting preparedDistance, BooleanSetting preparedSilentRotations) {
        maxPredictTicks = preparedMaxPredictTicks;
        distance = preparedDistance;
        silentRotations = preparedSilentRotations;
    }
}
