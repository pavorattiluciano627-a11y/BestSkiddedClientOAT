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
import net.minecraft.entity.Entity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

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
    private PlayerEntity target;
    private int selectedAtPlayerTick;
    private int selectedAtTrackerTick;
    private boolean delayedForWindCharge;
    private int riseStartTrackerTick;
    private TargetPath targetPath;
    private MovingTargetTrajectorySearch.Result targetMotion;
    private MovingTargetLaunch bestLaunch;
    private int pathUpdatedPlayerTick;
    private Vec3d lastSentPosition;
    private Vec3d sentVelocity;
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
        sentVelocity = Vec3d.ZERO;
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
        Vec3d position;
        if (client.player != null) {
            initializePearlClientType();
            position = client.player.getEntityPos();
        } else position = null;
        initializePearlVectorType();
        lastSentPosition = position;
        initializePearlClientType();
        Vec3d movement;
        if (client.player != null) {
            initializePearlClientType();
            Vec3d current = client.player.getEntityPos();
            initializePearlClientType();
            double previousX = client.player.lastX;
            initializePearlClientType();
            double previousY = client.player.lastY;
            initializePearlClientType();
            double previousZ = client.player.lastZ;
            movement = current.subtract(previousX, previousY, previousZ);
        } else movement = Vec3d.ZERO;
        initializePearlVectorType();
        sentVelocity = movement;
        initializePearlClientType();
        boolean onGround;
        if (client.player != null) {
            initializePearlClientType();
            onGround = client.player.isOnGround();
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
        int slot = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.ENDER_PEARL));
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
        selectedAtPlayerTick = client.player.age;
        selectedAtTrackerTick = EntityPositionTracker.currentTick();
        delayedForWindCharge = false;
        riseStartTrackerTick = -1;
        phase = PearlGrapplePhase.AIM;
    }

    private void aim() {
        int tick = client.player.age;
        if (!client.player.getMainHandStack().isOf(Items.ENDER_PEARL)) {
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
            float yaw = client.player.getYaw();
            float pitch = client.player.getPitch();
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

    private boolean shouldWaitForWindCharge(PlayerEntity player) {
        VelocitySample velocity = EntityPositionTracker.velocity(player);
        if (velocity != null && velocity.tick() >= selectedAtTrackerTick
                && velocity.velocity().lengthSquared() > VELOCITY_PACKET_THRESHOLD_SQUARED) return false;
        if (targetMotion != null && targetMotion.velocity().y > RISE_VELOCITY) {
            if (riseStartTrackerTick < 0) riseStartTrackerTick = EntityPositionTracker.currentTick();
            return false;
        }
        boolean holdsWindCharge = player.getMainHandStack().isOf(Items.WIND_CHARGE)
            || player.getOffHandStack().isOf(Items.WIND_CHARGE);
        return holdsWindCharge && player.getPitch() >= WIND_CHARGE_PITCH;
    }

    private boolean requiresRobustPrediction() {
        double speed = LatencyTracker.milliseconds() > HIGH_LATENCY_MILLIS ? HIGH_LATENCY_SPEED : FAST_TARGET_SPEED;
        if (targetMotion.velocity().length() > speed) return true;
        Vec3d velocity = targetMotion.velocity();
        return sentVelocity.y < FAST_FALL_VELOCITY && !sentOnGround && velocity.horizontalLength() > Math.abs(velocity.y);
    }

    private boolean remainsReliableWhenPerturbed(MovingTargetLaunch launch) {
        boolean falling = sentVelocity.y < FAST_FALL_VELOCITY && !sentOnGround;
        if (launch.hitFraction() < (falling ? FALLING_HIT_FRACTION : ORDINARY_MOVING_HIT_FRACTION)) return false;
        double age = predictionAge(targetMotion);
        int ticks = maxPredictTicks.getValue();
        for (double offset : new double[] {-PREDICTION_PERTURBATION_TICKS, PREDICTION_PERTURBATION_TICKS}) {
            Box[] boxes = MovingTargetTrajectorySearch.predictBoxes(target, targetMotion, age + offset, ticks);
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
        return client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL));
    }

    @Subscribe
    private void onMouseMovement(MouseMovementEvent.Post event) {
        if (isOutsideWorld() || silentRotations.getValue() || phase != PearlGrapplePhase.AIM) return;
        if (bestLaunch == null) {
            rotations().cancel(this);
            return;
        }
        Vec3d point = client.player.getEyePos().add(Vec3d.fromPolar(bestLaunch.pitch(), bestLaunch.yaw()).multiply(AIM_POINT_DISTANCE));
        float yawDelta = Math.abs(MathHelper.wrapDegrees(bestLaunch.yaw() - client.player.getYaw()));
        float pitchDelta = Math.abs(bestLaunch.pitch() - client.player.getPitch());
        float speed = MathHelper.clamp(Math.max(yawDelta, pitchDelta) / 0.1f, MIN_ROTATION_SPEED, MAX_ROTATION_SPEED);
        rotations().aim(this, RotationPriority.HIGH, point,
            RotationSettings.uniform(RotationVisibility.VISIBLE, AimTransition.NATURAL, speed));
    }

    @Subscribe
    private void onPacketSend(PacketSendEvent event) {
        if (client.player == null) return;
        if (event.getPacket() instanceof PlayerMoveC2SPacket movement) {
            if (movement.changesPosition()) {
                Vec3d position = new Vec3d(movement.getX(0.0), movement.getY(0.0), movement.getZ(0.0));
                sentVelocity = lastSentPosition == null ? client.player.getVelocity() : position.subtract(lastSentPosition);
                lastSentPosition = position;
            } else {
                sentVelocity = Vec3d.ZERO;
            }
            sentOnGround = movement.isOnGround();
            sentMovementThisTick = true;
        } else if (event.getPacket() instanceof ClientTickEndC2SPacket) {
            if (!sentMovementThisTick) sentVelocity = Vec3d.ZERO;
            sentMovementThisTick = false;
            if (phase == PearlGrapplePhase.AIM && isValidTarget(target)) {
                targetMotion = estimateMotion(target);
                targetPath = buildTargetPath(target, targetMotion);
                bestLaunch = MovingTargetTrajectorySearch.findLaunch(targetPath);
                pathUpdatedPlayerTick = client.player.age;
            }
        } else if (event.getPacket() instanceof TeleportConfirmC2SPacket) {
            lastSentPosition = null;
            sentVelocity = Vec3d.ZERO;
        }
    }

    private TargetPath buildTargetPath(PlayerEntity player, MovingTargetTrajectorySearch.Result motion) {
        Vec3d origin = lastSentPosition != null ? lastSentPosition : client.player.getEntityPos();
        Vec3d eye = origin.add(0.0, client.player.getStandingEyeHeight() - EYE_OFFSET, 0.0);
        Vec3d inherited = new Vec3d(sentVelocity.x, sentOnGround ? 0.0 : sentVelocity.y, sentVelocity.z);
        int ticks = maxPredictTicks.getValue();
        double age = predictionAge(motion);
        Box[] boxes = MovingTargetTrajectorySearch.predictBoxes(player, motion, age, ticks);
        return new TargetPath(eye, inherited, boxes, blockers(player));
    }

    private static MovingTargetTrajectorySearch.Result estimateMotion(PlayerEntity player) {
        int tick = EntityPositionTracker.currentTick();
        VelocitySample velocity = EntityPositionTracker.velocity(player);
        List<PositionSample> history = EntityPositionTracker.positions(player);
        return MovingTargetTrajectorySearch.estimateMotion(player, history, velocity, tick);
    }

    private static double predictionAge(MovingTargetTrajectorySearch.Result motion) {
        return (double) (EntityPositionTracker.currentTick() + 1 - motion.tick()) + (double) LatencyTracker.milliseconds() / 50.0;
    }

    private List<Box> blockers(PlayerEntity player) {
        return client.world.getOtherEntities(client.player, client.player.getBoundingBox().expand(targetDistance()),
            entity -> entity != player && entity.canBeHitByProjectile() && !(entity instanceof ProjectileEntity))
            .stream().map(Entity::getBoundingBox).toList();
    }

    private PlayerEntity selectTarget() {
        PlayerEntity best = null;
        double bestFraction = -1.0;
        double bestDistance = Double.MAX_VALUE;
        for (PlayerEntity player : client.world.getPlayers()) {
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

    private boolean isValidTarget(PlayerEntity player) {
        return player != null && player != client.player && TargetSelection.isValidTarget(player)
            && client.player.distanceTo(player) <= targetDistance() && !player.hasVehicle()
            && !player.isUsingRiptide() && !recentlyTeleported(player);
    }

    private static boolean recentlyTeleported(PlayerEntity player) {
        List<PositionSample> history = EntityPositionTracker.positions(player);
        for (int i = 1; i < history.size(); i++) {
            if (EntityPositionTracker.currentTick() - history.get(i).tick() > TELEPORT_SAMPLE_TICKS) continue;
            if (history.get(i).position().distanceTo(history.get(i - 1).position()) > TELEPORT_DISTANCE) return true;
        }
        return false;
    }

    private static float targetDistance() { return Math.min(distance.getValue(), MAX_TARGET_DISTANCE); }

    private static void initializePearlPlayerType() {
        try { Class.forName(PlayerEntity.class.getName(), true, PlayerEntity.class.getClassLoader()); }
        catch (ClassNotFoundException failure) { throw new IllegalStateException("Resolved class initialization failed", failure); }
    }

    private static void initializePearlClientType() {
        try { Class.forName(MinecraftClient.class.getName(), true, MinecraftClient.class.getClassLoader()); }
        catch (ClassNotFoundException failure) { throw new IllegalStateException("Resolved class initialization failed", failure); }
    }

    private static void initializePearlVectorType() {
        try { Class.forName(Vec3d.class.getName(), true, Vec3d.class.getClassLoader()); }
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
