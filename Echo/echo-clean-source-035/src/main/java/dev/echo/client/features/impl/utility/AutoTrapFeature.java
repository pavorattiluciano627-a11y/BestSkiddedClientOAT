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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
            CatalogText.literal("Blocks"), () -> BuiltInRegistries.BLOCK, () -> mode, () -> modeOption2);
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
        if (!block.defaultBlockState().isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
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
        trapComplete = placementSearch().plan(target, Vec3.ZERO).isEmpty();
        if (trapComplete) {
            primaryPlacement = null;
            return;
        }
        if (!isStationaryTarget(target)) {
            primaryPlacement = null;
            secondaryPlacement = null;
            return;
        }
        Vec3 displacement = predictHorizontalOffset(target, TARGET_PREDICTION_EXTRA_TICKS);
        List<PlacementFilter> plan = displacement.horizontalDistanceSqr() > MINIMUM_PREDICTED_MOVEMENT_SQUARED
                ? placementSearch().planPredicted(target, displacement)
                : placementSearch().plan(target, Vec3.ZERO);
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
        if (jumpRequested && !isOutsideWorld() && client.player.onGround()) {
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
        Vec3 point = pendingPlacement.target().hitResult().getLocation().subtract(predictionVelocity());
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
        if (!plan.itemFilter().test(client.player.getMainHandItem()) || !isStillValid(plan)) {
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
        InteractionResult result = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        if (result instanceof InteractionResult.Success success && success.swingSource() == InteractionResult.SwingSource.CLIENT) {
            client.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private TrapPlacementPlan placementAbove(TrapPlacementPlan first, Set<BlockPos> positions) {
        BlockPos base = first.target().position();
        BlockPos above = base.above();
        if (!positions.contains(above) || !PlacementWorldQueries.isReplaceable(above)) {
            return null;
        }
        Vec3 point = Vec3.atCenterOf(base).add(0.0, 0.5, 0.0);
        Vec3 eye = client.player.getEyePosition();
        if (eye.y < point.y || eye.distanceToSqr(point) > Mth.square(client.player.blockInteractionRange())) {
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
        return PlacementWorldQueries.canPlaceStack(client.player.getMainHandItem(), plan.target().hitResult())
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
        Vec3 eye = client.player.getEyePosition(1.0F);
        Vec3 end = eye.add(rotationVector(rotation.yaw(), rotation.pitch()).scale(client.player.blockInteractionRange()));
        if (plan.ignoreInterveningBlocks()) {
            return new AABB(expected.getBlockPos()).inflate(1.0E-4).clip(eye, end).isPresent() ? expected : null;
        }
        BlockHitResult actual = client.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, client.player));
        if (actual == null || actual.getType() != HitResult.Type.BLOCK
                || !actual.getBlockPos().equals(expected.getBlockPos()) || actual.getDirection() != expected.getDirection()) {
            return null;
        }
        return actual;
    }

    private TrapPlacementPlan choosePlacement(List<PlacementFilter> candidates, BlockPos excluded) {
        TrapPlacementPlan visible = choosePlacement(candidates, excluded, true);
        return visible != null ? visible : choosePlacement(candidates, excluded, false);
    }

    private TrapPlacementPlan choosePlacement(List<PlacementFilter> candidates, BlockPos excluded, boolean requireVisible) {
        Vec3 eye = client.player.getEyePosition();
        double reachSquared = Mth.square(client.player.blockInteractionRange());
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
            if (placement == null || eye.distanceToSqr(placement.hitResult().getLocation()) > reachSquared) {
                continue;
            }
            if (!PlacementWorldQueries.canPlaceStack(client.player.getInventory().getItem(slot), placement.hitResult())
                    && !isClearOnArrival(candidate.position())) {
                continue;
            }
            return new TrapPlacementPlan(placement, candidate.acceptsStack(), slot, !requireVisible);
        }
        return null;
    }

    private boolean isClearOnArrival(BlockPos position) {
        AABB block = new AABB(position);
        if (target.getBoundingBox().intersects(block) || predictedVerticalBounds(target).intersects(block)) {
            return false;
        }
        return client.level.getEntities(target, block, entity -> entity.blocksBuilding).isEmpty();
    }

    private static AABB predictedVerticalBounds(LivingEntity entity) {
        int ticks = (int) Math.round(PREDICTION_BASE_TICKS + LatencyTracker.milliseconds() / 50.0);
        double floor = PlacementBounds.around(entity, Vec3.ZERO, PlacementWorldQueries::isReplaceable).minY();
        double y = entity.getY();
        double velocityY = entity.getY() - entity.yo;
        for (int tick = 0; tick < ticks; tick++) {
            velocityY = (velocityY - GRAVITY) * VERTICAL_DRAG;
            y += velocityY;
            if (y <= floor) {
                y = floor;
                velocityY = 0.0;
            }
        }
        return entity.getBoundingBox().move(0.0, y - entity.getY(), 0.0);
    }

    private static Vec3 predictHorizontalOffset(LivingEntity entity, double extraTicks) {
        int ticks = (int) Math.round(PREDICTION_BASE_TICKS + extraTicks + LatencyTracker.milliseconds() / 50.0);
        Vec3 displacement = predictBounds(entity, ticks, !entity.onGround()).getCenter()
                .subtract(entity.getBoundingBox().getCenter());
        return new Vec3(displacement.x, 0.0, displacement.z);
    }

    private static AABB predictBounds(LivingEntity entity, int ticks, boolean jump) {
        Vec3 velocity = entity.position().subtract(entity.xo, entity.yo, entity.zo);
        if (entity instanceof Player player && client.level != null) {
            try {
                SimulatedPlayer simulation = PlayerPrediction.forWorld(client.level);
                boolean moving = velocity.horizontalDistanceSqr() > STATIONARY_DISTANCE_SQUARED;
                float yaw = moving ? (float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z)) : player.getYRot();
                simulation.copyFrom(player, velocity, yaw, 0.0F, moving ? 1.0F : 0.0F, 0.0F, jump);
                for (int tick = 0; tick < ticks; tick++) {
                    simulation.tickPrediction();
                }
                return simulation.getBoundingBox();
            } catch (Throwable simulationFailure) {
                // The artifact falls back to horizontal extrapolation when simulation fails.
            }
        }
        return entity.getBoundingBox().move(velocity.x * ticks, 0.0, velocity.z * ticks);
    }

    private Vec3 predictionVelocity() {
        Vec3 velocity = client.player.getDeltaMovement();
        double y = client.player.onGround() ? (jumpRequested ? JUMP_VELOCITY : 0.0) : velocity.y;
        return new Vec3(velocity.x, y, velocity.z);
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
        List<LivingEntity> candidates = client.level.getEntitiesOfClass(LivingEntity.class,
                client.player.getBoundingBox().inflate(targetRange()),
                entity -> entity != client.player && entity.isAlive() && TargetSelection.isValidTarget(entity)
                        && isStationaryTarget(entity));
        LivingEntity closest = candidates.stream().min(Comparator.comparingDouble(client.player::distanceToSqr)).orElse(null);
        return TargetSelection.resolveTarget(closest);
    }

    private static boolean isStationaryTarget(LivingEntity entity) {
        if (!onlyStationary.getValue()) {
            return true;
        }
        double distance = Mth.square(entity.getX() - entity.xo) + Mth.square(entity.getZ() - entity.zo);
        return distance < STATIONARY_DISTANCE_SQUARED || isWebbed(entity);
    }

    private static boolean isWebbed(LivingEntity entity) {
        return BlockPos.betweenClosedStream(entity.getBoundingBox().deflate(1.0E-4))
                .anyMatch(position -> client.level.getBlockState(position).is(Blocks.COBWEB));
    }

    private boolean isCurrentTargetValid(LivingEntity entity) {
        return entity != null && entity.isAlive() && client.player.distanceToSqr(entity) <= Mth.square(targetRange());
    }

    private double targetRange() {
        return client.player.blockInteractionRange() + TARGET_RANGE_PADDING;
    }

    private void finishDeferredDisable() {
        if (rotations().beginReturn(this)) {
            return;
        }
        disablePending = false;
        super.onDisable();
    }

    /** Preserves Entity.getRotationVector's float intermediates and trig call order. */
    private static Vec3 rotationVector(float yaw, float pitch) {
        float pitchRadians = pitch * 0.017453292F;
        float yawRadians = -yaw * 0.017453292F;
        float cosineYaw = Mth.cos(yawRadians);
        float sineYaw = Mth.sin(yawRadians);
        float cosinePitch = Mth.cos(pitchRadians);
        float sinePitch = Mth.sin(pitchRadians);
        return new Vec3(sineYaw * cosinePitch, -sinePitch, cosineYaw * cosinePitch);
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
