package dev.echo.client.features.impl.utility;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HotbarKeyEvent;
import dev.echo.client.events.input.MouseMovementEvent;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.events.input.UseKeyEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.movement.MovementPacketEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.handlers.AimTransition;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.handlers.LatencyTracker;
import dev.echo.client.handlers.RotationPriority;
import dev.echo.client.handlers.RotationSettings;
import dev.echo.client.handlers.RotationVisibility;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.setting.RegistryListSetting;
import dev.echo.client.utils.Rotation;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.placement.FilteredPlacementSearch;
import dev.echo.client.utils.placement.NearestPlacementSearch;
import dev.echo.client.utils.placement.PlacementBounds;
import dev.echo.client.utils.placement.PlacementFilter;
import dev.echo.client.utils.placement.PlacementSearch;
import dev.echo.client.utils.placement.PlacementTarget;
import dev.echo.client.utils.placement.PlacementTargets;
import dev.echo.client.utils.placement.PlacementWorldQueries;
import dev.echo.client.utils.simulation.PlayerPrediction;
import dev.echo.client.utils.simulation.SimulatedPlayer;
import dev.echo.client.utils.targeting.TargetSelection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.RaycastContext;

/** Plans, aims and places trap blocks around a selected target. */
public class AutoTrapFeature extends Feature {
    private static final float ROTATION_SPEED = 200.0F;
    private static final float UNUSED_FLOAT_CONSTANT = 1.7F;
    private static final double TARGET_RANGE_PADDING = 2.0;
    private static final int COMPLETION_GRACE_TICKS = 2;
    private static final double TARGET_PREDICTION_EXTRA_TICKS = 4.0;
    private static final double MINIMUM_PREDICTED_MOVEMENT_SQUARED = 0.01;
    private static final int MAX_BELOW_EYE_WITHOUT_PRIMARY = 2;
    private static final double STATIONARY_DISTANCE_SQUARED = 9.0E-4;
    private static final double GRAVITY = 0.08;
    private static final double VERTICAL_DRAG = 0.98;
    private static final double JUMP_VELOCITY = 0.42;
    private static final double PREDICTION_BASE_TICKS = 4.0;
    private static final CharSequence modeOption2 = CatalogText.literal("Block");
    private static final CharSequence modeOption = CatalogText.literal("Walls");
    private static ChoiceSetting mode;
    private static RegistryListSetting<Block> blocks = new AutoTrapBlockSelectionSetting(
            CatalogText.literal("Blocks"), () -> Registries.BLOCK, () -> mode, () -> modeOption2);
    private static BooleanSetting silentRotations;
    private static BooleanSetting onlyStationary;
    private static IntSetting placeDelay;

    private PlacementSearch fullBlockSearch;
    private PlacementSearch wallSearch;
    private LivingEntity target;
    private TrapPlacementPlan primaryPlacement;
    private TrapPlacementPlan secondaryPlacement;
    private TrapPlacementPlan pendingPlacement;
    private TrapPlacementPlan lastPlacement;
    private Set<BlockPos> candidatePositions;
    private int placementCooldownTicks;
    private boolean disablePending;
    private boolean jumpRequested;
    private boolean trapComplete;
    private BlockHitResult pendingInteractionHit;
    private int completionTicks;

    public AutoTrapFeature() {
        super(new dev.echo.client.features.FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Auto Trap"), dev.echo.client.bootstrap.CatalogText.literal("Automatically surrounds targets with blocks"), dev.echo.client.features.Category.UTILITY));
    }

    @Override
    public void postInitialize() {
        fullBlockSearch = new FilteredPlacementSearch(AutoTrapFeature::matchesTrapBlock);
        wallSearch = new NearestPlacementSearch();
        candidatePositions = Set.of();
        blocks.description(CatalogText.literal("Full blocks to trap with. An empty selection allows every full block"));
        onlyStationary.description(CatalogText.literal("Only traps targets that are standing still or stuck in a cobweb"));
        placeDelay.tooltip("Timing");
    }

    private PlacementSearch placementSearch() {
        return mode.isSelected(modeOption2) ? fullBlockSearch : wallSearch;
    }

    private static boolean matchesTrapBlock(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) {
            return false;
        }
        Block block = item.getBlock();
        if (!block.getDefaultState().isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) {
            return false;
        }
        return blocks.getSelectedCount() == 0 || blocks.isEntrySelected(block);
    }

    @Override
    public void onEnable() {
        target = null;
        primaryPlacement = null;
        secondaryPlacement = null;
        pendingPlacement = null;
        lastPlacement = null;
        candidatePositions = Set.of();
        placementCooldownTicks = 0;
        disablePending = false;
        jumpRequested = false;
        trapComplete = false;
        completionTicks = 0;
        pendingInteractionHit = null;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        target = null;
        primaryPlacement = null;
        secondaryPlacement = null;
        pendingPlacement = null;
        lastPlacement = null;
        jumpRequested = false;
        DeferredActionQueue.finish(this, true);
        if (silentRotations.getValue() && rotations().isOwnedBy(this)) {
            disablePending = true;
            return;
        }
        disablePending = false;
        rotations().cancel(this);
        super.onDisable();
    }

    @Subscribe
    private void onTick(ClientTickEvent event) {
        if (placementCooldownTicks > 0) {
            placementCooldownTicks--;
        }
        jumpRequested = false;
        if (disablePending) {
            return;
        }
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            return;
        }
        updatePlan();
        completionTicks = trapComplete ? completionTicks + 1 : 0;
        if (completionTicks > COMPLETION_GRACE_TICKS + LatencyTracker.milliseconds() / 50) {
            setEnabled(false);
        }
    }

    private void updatePlan() {
        if (!isCurrentTargetValid(target)) {
            target = findTarget();
        }
        if (target == null) {
            primaryPlacement = null;
            trapComplete = false;
            return;
        }
        trapComplete = placementSearch().plan(target, Vec3d.ZERO).isEmpty();
        if (trapComplete) {
            primaryPlacement = null;
            return;
        }
        if (!isStationaryTarget(target)) {
            primaryPlacement = null;
            secondaryPlacement = null;
            return;
        }
        Vec3d displacement = predictHorizontalOffset(target, TARGET_PREDICTION_EXTRA_TICKS);
        List<PlacementFilter> plan = displacement.horizontalLengthSquared() > MINIMUM_PREDICTED_MOVEMENT_SQUARED
                ? placementSearch().planPredicted(target, displacement)
                : placementSearch().plan(target, Vec3d.ZERO);
        primaryPlacement = choosePlacement(plan, null);
        candidatePositions = plan.stream().map(PlacementFilter::position).collect(Collectors.toSet());
        secondaryPlacement = primaryPlacement == null ? null : choosePlacement(plan, primaryPlacement.target().position());
        if (secondaryPlacement == null && primaryPlacement != null) {
            secondaryPlacement = placementAbove(primaryPlacement, candidatePositions);
        }
        jumpRequested = hasPlacementAboveEye(plan)
                && (primaryPlacement != null || countPlacementsBelowEye(plan) <= MAX_BELOW_EYE_WITHOUT_PRIMARY);
    }

    @Subscribe
    private void onPlayerInput(PlayerInputEvent event) {
        if (jumpRequested && !isOutsideWorld() && client.player.isOnGround()) {
            event.jump = true;
        }
    }

    @Subscribe
    private void onMouseMovement(MouseMovementEvent.Post event) {
        if (disablePending) {
            finishDeferredDisable();
            return;
        }
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            rotations().beginReturn(this);
            return;
        }
        TrapPlacementPlan first = primaryPlacement;
        if (first != null && !PlacementWorldQueries.isReplaceable(first.target().position())) {
            updatePlan();
            first = primaryPlacement;
        }
        if (first == null) {
            pendingPlacement = null;
            releaseOrCancelRotation();
            return;
        }
        pendingPlacement = secondaryPlacement != null && samePosition(first, lastPlacement) ? secondaryPlacement : first;
        Vec3d point = pendingPlacement.target().hitResult().getPos().subtract(predictionVelocity());
        if (!rotations().aim(this, RotationPriority.HIGH, point, RotationSettings.uniform(
                silentRotations.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE,
                AimTransition.SNAP, ROTATION_SPEED))) {
            pendingPlacement = null;
            releaseOrCancelRotation();
        }
    }

    private void releaseOrCancelRotation() {
        if (silentRotations.getValue()) {
            rotations().beginReturn(this);
        } else {
            rotations().cancel(this);
        }
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        TrapPlacementPlan first = primaryPlacement;
        if (first == null || isOutsideWorld() || disablePending || event.slot != first.hotbarSlot()) {
            return;
        }
        if (DeferredActionQueue.selectedSlot() == first.hotbarSlot()) {
            return;
        }
        if (DeferredActionQueue.ensureSelected(this, first.hotbarSlot(), -1, true)) {
            event.addClick();
        }
    }

    @Subscribe
    private void onUseKey(UseKeyEvent event) {
        TrapPlacementPlan plan = lastPlacement;
        if (plan == null || isOutsideWorld() || disablePending || MinecraftRenderState.getScreen() != null) {
            return;
        }
        if (placementCooldownTicks > 0 || event.hasClicks()) {
            return;
        }
        if (!plan.itemFilter().test(client.player.getMainHandStack()) || !isStillValid(plan)) {
            return;
        }
        if (!rotations().isRotationReady()) {
            return;
        }
        BlockHitResult hit = interactionHit(plan);
        if (hit == null) {
            return;
        }
        pendingInteractionHit = hit;
        placementCooldownTicks = placeDelay.getValue();
        rotations().acknowledgeRotation();
    }

    @Subscribe
    private void onMovementPacket(MovementPacketEvent.Pre event) {
        BlockHitResult hit = pendingInteractionHit;
        pendingInteractionHit = null;
        lastPlacement = pendingPlacement;
        if (hit == null || isOutsideWorld()) {
            return;
        }
        ActionResult result = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
        if (result instanceof ActionResult.Success success && success.swingSource() == ActionResult.SwingSource.CLIENT) {
            client.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private TrapPlacementPlan placementAbove(TrapPlacementPlan first, Set<BlockPos> positions) {
        BlockPos base = first.target().position();
        BlockPos above = base.up();
        if (!positions.contains(above) || !PlacementWorldQueries.isReplaceable(above)) {
            return null;
        }
        Vec3d point = Vec3d.ofCenter(base).add(0.0, 0.5, 0.0);
        Vec3d eye = client.player.getEyePos();
        if (eye.y < point.y || eye.squaredDistanceTo(point) > MathHelper.square(client.player.getBlockInteractionRange())) {
            return null;
        }
        BlockHitResult hit = new BlockHitResult(point, Direction.UP, base, false);
        return new TrapPlacementPlan(new PlacementTarget(above, hit), first.itemFilter(), first.hotbarSlot(), false);
    }

    private boolean isStillValid(TrapPlacementPlan plan) {
        BlockPos position = plan.target().position();
        if (!candidatePositions.contains(position) || !PlacementWorldQueries.isReplaceable(position)) {
            return false;
        }
        return PlacementWorldQueries.canPlaceStack(client.player.getMainHandStack(), plan.target().hitResult())
                || isClearOnArrival(position);
    }

    private static boolean samePosition(TrapPlacementPlan first, TrapPlacementPlan second) {
        return second != null && first.target().position().equals(second.target().position());
    }

    private BlockHitResult interactionHit(TrapPlacementPlan plan) {
        Rotation rotation = rotations().serverRotation();
        if (rotation == null) {
            return null;
        }
        BlockHitResult expected = plan.target().hitResult();
        Vec3d eye = client.player.getCameraPosVec(1.0F);
        Vec3d end = eye.add(rotationVector(rotation.yaw(), rotation.pitch()).multiply(client.player.getBlockInteractionRange()));
        if (plan.ignoreInterveningBlocks()) {
            return new Box(expected.getBlockPos()).expand(1.0E-4).raycast(eye, end).isPresent() ? expected : null;
        }
        BlockHitResult actual = client.world.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE, client.player));
        if (actual == null || actual.getType() != HitResult.Type.BLOCK
                || !actual.getBlockPos().equals(expected.getBlockPos()) || actual.getSide() != expected.getSide()) {
            return null;
        }
        return actual;
    }

    private TrapPlacementPlan choosePlacement(List<PlacementFilter> candidates, BlockPos excluded) {
        TrapPlacementPlan visible = choosePlacement(candidates, excluded, true);
        return visible != null ? visible : choosePlacement(candidates, excluded, false);
    }

    private TrapPlacementPlan choosePlacement(List<PlacementFilter> candidates, BlockPos excluded, boolean requireVisible) {
        Vec3d eye = client.player.getEyePos();
        double reachSquared = MathHelper.square(client.player.getBlockInteractionRange());
        for (PlacementFilter candidate : candidates) {
            if (candidate.position().equals(excluded)) {
                continue;
            }
            int slot = InventoryItems.findStackInHotbar(candidate.acceptsStack());
            if (slot == -1) {
                continue;
            }
            PlacementTarget placement = PlacementTargets.find(candidate.position(), requireVisible,
                    placementSearch().allowsPartialSupport(), true);
            if (placement == null || eye.squaredDistanceTo(placement.hitResult().getPos()) > reachSquared) {
                continue;
            }
            if (!PlacementWorldQueries.canPlaceStack(client.player.getInventory().getStack(slot), placement.hitResult())
                    && !isClearOnArrival(candidate.position())) {
                continue;
            }
            return new TrapPlacementPlan(placement, candidate.acceptsStack(), slot, !requireVisible);
        }
        return null;
    }

    private boolean isClearOnArrival(BlockPos position) {
        Box block = new Box(position);
        if (target.getBoundingBox().intersects(block) || predictedVerticalBounds(target).intersects(block)) {
            return false;
        }
        return client.world.getOtherEntities(target, block, entity -> entity.intersectionChecked).isEmpty();
    }

    private static Box predictedVerticalBounds(LivingEntity entity) {
        int ticks = (int) Math.round(PREDICTION_BASE_TICKS + LatencyTracker.milliseconds() / 50.0);
        double floor = PlacementBounds.around(entity, Vec3d.ZERO, PlacementWorldQueries::isReplaceable).minY();
        double y = entity.getY();
        double velocityY = entity.getY() - entity.lastY;
        for (int tick = 0; tick < ticks; tick++) {
            velocityY = (velocityY - GRAVITY) * VERTICAL_DRAG;
            y += velocityY;
            if (y <= floor) {
                y = floor;
                velocityY = 0.0;
            }
        }
        return entity.getBoundingBox().offset(0.0, y - entity.getY(), 0.0);
    }

    private static Vec3d predictHorizontalOffset(LivingEntity entity, double extraTicks) {
        int ticks = (int) Math.round(PREDICTION_BASE_TICKS + extraTicks + LatencyTracker.milliseconds() / 50.0);
        Vec3d displacement = predictBounds(entity, ticks, !entity.isOnGround()).getCenter()
                .subtract(entity.getBoundingBox().getCenter());
        return new Vec3d(displacement.x, 0.0, displacement.z);
    }

    private static Box predictBounds(LivingEntity entity, int ticks, boolean jump) {
        Vec3d velocity = entity.getEntityPos().subtract(entity.lastX, entity.lastY, entity.lastZ);
        if (entity instanceof PlayerEntity player && client.world != null) {
            try {
                SimulatedPlayer simulation = PlayerPrediction.forWorld(client.world);
                boolean moving = velocity.horizontalLengthSquared() > STATIONARY_DISTANCE_SQUARED;
                float yaw = moving ? (float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z)) : player.getYaw();
                simulation.copyFrom(player, velocity, yaw, 0.0F, moving ? 1.0F : 0.0F, 0.0F, jump);
                for (int tick = 0; tick < ticks; tick++) {
                    simulation.tickPrediction();
                }
                return simulation.getBoundingBox();
            } catch (Throwable simulationFailure) {
                // The artifact falls back to horizontal extrapolation when simulation fails.
            }
        }
        return entity.getBoundingBox().offset(velocity.x * ticks, 0.0, velocity.z * ticks);
    }

    private Vec3d predictionVelocity() {
        Vec3d velocity = client.player.getVelocity();
        double y = client.player.isOnGround() ? (jumpRequested ? JUMP_VELOCITY : 0.0) : velocity.y;
        return new Vec3d(velocity.x, y, velocity.z);
    }

    private boolean hasPlacementAboveEye(List<PlacementFilter> positions) {
        double eyeY = client.player.getEyeY();
        return positions.stream().anyMatch(position -> position.position().getY() > eyeY - 0.03);
    }

    private int countPlacementsBelowEye(List<PlacementFilter> positions) {
        double eyeY = client.player.getEyeY();
        return (int) positions.stream().filter(position -> position.position().getY() <= eyeY - 0.03).count();
    }

    private LivingEntity findTarget() {
        List<LivingEntity> candidates = client.world.getEntitiesByClass(LivingEntity.class,
                client.player.getBoundingBox().expand(targetRange()),
                entity -> entity != client.player && entity.isAlive() && TargetSelection.isValidTarget(entity)
                        && isStationaryTarget(entity));
        LivingEntity closest = candidates.stream().min(Comparator.comparingDouble(client.player::squaredDistanceTo)).orElse(null);
        return TargetSelection.resolveTarget(closest);
    }

    private static boolean isStationaryTarget(LivingEntity entity) {
        if (!onlyStationary.getValue()) {
            return true;
        }
        double distance = MathHelper.square(entity.getX() - entity.lastX) + MathHelper.square(entity.getZ() - entity.lastZ);
        return distance < STATIONARY_DISTANCE_SQUARED || isWebbed(entity);
    }

    private static boolean isWebbed(LivingEntity entity) {
        return BlockPos.stream(entity.getBoundingBox().contract(1.0E-4))
                .anyMatch(position -> client.world.getBlockState(position).isOf(Blocks.COBWEB));
    }

    private boolean isCurrentTargetValid(LivingEntity entity) {
        return entity != null && entity.isAlive() && client.player.squaredDistanceTo(entity) <= MathHelper.square(targetRange());
    }

    private double targetRange() {
        return client.player.getBlockInteractionRange() + TARGET_RANGE_PADDING;
    }

    private void finishDeferredDisable() {
        if (rotations().beginReturn(this)) {
            return;
        }
        disablePending = false;
        super.onDisable();
    }

    /** Preserves Entity.getRotationVector's float intermediates and trig call order. */
    private static Vec3d rotationVector(float yaw, float pitch) {
        float pitchRadians = pitch * 0.017453292F;
        float yawRadians = -yaw * 0.017453292F;
        float cosineYaw = MathHelper.cos(yawRadians);
        float sineYaw = MathHelper.sin(yawRadians);
        float cosinePitch = MathHelper.cos(pitchRadians);
        float sinePitch = MathHelper.sin(pitchRadians);
        return new Vec3d(sineYaw * cosinePitch, -sinePitch, cosineYaw * cosinePitch);
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(AutoTrapFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoTrapFeature.mode);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoTrapFeature.blocks);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoTrapFeature.silentRotations);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoTrapFeature.onlyStationary);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoTrapFeature.placeDelay);
    }

    /** Reads the shared modeOption2 default without copying its value. */
    public static java.lang.CharSequence bootstrapModeOption2() {
        return modeOption2;
    }

    /** Reads the shared modeOption default without copying its value. */
    public static java.lang.CharSequence bootstrapModeOption() {
        return modeOption;
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(AutoTrapFeature feature, dev.echo.client.setting.BooleanSetting preparedSilentRotations, dev.echo.client.setting.IntSetting preparedPlaceDelay, dev.echo.client.setting.BooleanSetting preparedOnlyStationary, dev.echo.client.setting.ChoiceSetting preparedMode) {
        AutoTrapFeature.silentRotations = preparedSilentRotations;
        AutoTrapFeature.placeDelay = preparedPlaceDelay;
        AutoTrapFeature.onlyStationary = preparedOnlyStationary;
        AutoTrapFeature.mode = preparedMode;
    }
}
