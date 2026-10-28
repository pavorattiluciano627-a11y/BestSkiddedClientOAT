package dev.echo.client.utils.placement;

import static dev.echo.client.MinecraftAccess.CLIENT;

import dev.echo.client.Echo;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Event;
import dev.echo.client.events.input.HotbarKeyEvent;
import dev.echo.client.events.input.MouseMovementEvent;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.events.input.UseKeyEvent;
import dev.echo.client.events.interaction.BlockInteractEvent;
import dev.echo.client.events.movement.MovementPacketEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.impl.world.ScaffoldFeature;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.handlers.RotationManager;
import dev.echo.client.handlers.RotationPriority;
import dev.echo.client.handlers.RotationSettings;
import dev.echo.client.handlers.RotationVisibility;
import dev.echo.client.handlers.AimTransition;
import dev.echo.client.mixin.PlayerEntityAccessor;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.setting.Setting;
import dev.echo.client.utils.Rotation;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.simulation.PlayerPrediction;
import dev.echo.client.utils.simulation.SimulatedPlayer;
import dev.echo.client.utils.targeting.BlockRaycasts;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Full bridging controller candidate; original caller/key/cache domains remain explicit. */
public class ScaffoldController {
    private static Event[] inputMarker = new Event[2];
    private static final CharSequence NEVER = CatalogText.literal("Never");
    private static final CharSequence ON_DEPLETION = CatalogText.literal("On Depletion");
    private static final CharSequence ALWAYS = CatalogText.literal("Always");
    private static final float MIN_ROTATION_SPEED = 150.0F;
    private static final float MAX_ROTATION_SPEED = 600.0F;
    private static final float MIN_PITCH = 30.0F;
    private static final float MAX_PITCH = 85.0F;
    private static final float PITCH_STEP = 1.0F;
    private static final double FACE_MARGIN = 0.1;
    private static final float ALIGNED_YAW_EPSILON = 0.3F;
    private static final float YAW_NUDGE = 0.5F;
    private static final float LARGE_YAW_CHANGE = 30.0F;
    private static final int YAW_SETTLE_TICKS = 5;
    private static final double MOTION_THRESHOLD = 0.05;
    private static final double LATERAL_HYSTERESIS = 0.1;
    private static final double FACE_HEIGHT_STEP = 0.1;
    public final FloatSetting edgeOffset = new FloatSetting(CatalogText.literal("Edge Offset"), 0.25F, 0.05F, 1.0F, 0.05F, CatalogText.literal(" blocks"));
    public final IntSetting unsneakDelay = new IntSetting(CatalogText.literal("Unsneak"), 2, 0, 20, CatalogText.literal(" ticks"));
    public final BooleanSetting requireSneak = new BooleanSetting(CatalogText.literal("Require Sneak"), false);
    public final BooleanSetting notMovingForward = new BooleanSetting(CatalogText.literal("Not Moving Forward"), false);
    public final BooleanSetting holdingBlocks = new BooleanSetting(CatalogText.literal("Holding Blocks"), true);
    public final ChoiceSetting blockSelection = new ChoiceSetting(CatalogText.literal("Block Selection"), NEVER, NEVER, ON_DEPLETION, ALWAYS);
    public final BooleanSetting autoRotate = new BooleanSetting(CatalogText.literal("Auto Rotate"), false);
    public final BooleanSetting autoPlace = new BooleanSetting(CatalogText.literal("Auto Place"), false);
    public final BooleanSetting increaseY = new BooleanSetting(CatalogText.literal("Increase Y"), true);
    private final Feature owner;
    private int lastPlacedTick = -1073741824;
    private int blockSlot = -1;
    private boolean sneakLatched;
    private boolean canAssist;
    private float inputYaw = Float.NaN;
    private float movementYaw = Float.NaN;
    private float desiredYaw = Float.NaN;
    private float diagonalOffset;
    private boolean cardinalMovement;
    private BlockPos lastSupport;
    private float previousYaw = Float.NaN;
    private int lastLargeYawChangeTick = -1073741824;
    private int bridgeY = Integer.MAX_VALUE;
    private PlacementTarget target;
    private float retainedPitch = Float.NaN;
    private float predictedPitch = Float.NaN;

    public ScaffoldController(Feature owner, Predicate<Object> visibility) {
        this.owner = owner;
        edgeOffset.description(CatalogText.literal("How far beyond where you will be next tick missing ground is detected"));
        unsneakDelay.description(CatalogText.literal("Delay after placing a block before sneak is released"));
        requireSneak.description(CatalogText.literal("Assist while you hold the sneak key."));
        notMovingForward.description(CatalogText.literal("Assist while the forward key is not pressed"));
        holdingBlocks.description(CatalogText.literal("Assist while holding an allowed block"));
        blockSelection.description(CatalogText.literal("When to switch your hotbar to an allowed block"));
        autoRotate.description(CatalogText.literal("Silently look down at the block you bridge from, diagonally backward on the ground and straight back in the air"));
        autoPlace.description(CatalogText.literal("Place when your view lands on the missing block under you"));
        increaseY.description(CatalogText.literal("Let the bridge level rise with you while jumping"));
        edgeOffset.tooltip("Sneak"); unsneakDelay.tooltip("Sneak");
        requireSneak.tooltip("Conditions"); notMovingForward.tooltip("Conditions"); holdingBlocks.tooltip("Conditions");
        blockSelection.tooltip("Blocks"); autoRotate.tooltip("Automation"); autoPlace.tooltip("Automation"); increaseY.tooltip("Automation");
        for (Setting setting : getSettings()) { setting.setVisibilityPredicate(visibility); }
    }

    public List<Setting> getSettings() { return List.of(edgeOffset, unsneakDelay, requireSneak, notMovingForward,
            holdingBlocks, blockSelection, autoRotate, autoPlace, increaseY); }

    public void reset() {
        lastPlacedTick = -1073741824; blockSlot = -1; sneakLatched = false; canAssist = false;
        inputYaw = Float.NaN; movementYaw = Float.NaN; desiredYaw = Float.NaN; diagonalOffset = 0.0F;
        previousYaw = Float.NaN; lastLargeYawChangeTick = -1073741824; bridgeY = Integer.MAX_VALUE;
        target = null; retainedPitch = Float.NaN; predictedPitch = Float.NaN; lastSupport = null;
        if (Event.getEventCounterFallbackValue() == 0) { setInputMarker(new Event[1]); }
    }

    public void onPlayerInput(PlayerInputEvent event) {
        blockSlot = -1; canAssist = false; target = null; predictedPitch = Float.NaN;
        if (owner.isOutsideWorld()) { return; }
        trackYawChange();
        if (!ScaffoldFeature.passesPitchCheck()) { return; }
        if (lastPlacedTick > CLIENT.player.age) { lastPlacedTick = -1073741824; }
        if (requireSneak.getValue() && !event.originalInput.sneak()) { return; }
        if (notMovingForward.getValue() && event.originalInput.forward()) { return; }
        boolean onGround = CLIENT.player.isOnGround();
        if (onGround) {
            if (needsSneak(event)) { sneakLatched = true; }
            else if (CLIENT.player.age - lastPlacedTick >= unsneakDelay.getValue()) { sneakLatched = false; }
        }
        ItemStack mainHand = CLIENT.player.getMainHandStack();
        if (sneakLatched && !blockSelection.isSelected(NEVER)) {
            boolean shouldSelect = blockSelection.isSelected(ALWAYS) ? !ScaffoldFeature.isAllowedStack(mainHand) : mainHand.isEmpty();
            if (shouldSelect) { blockSlot = InventoryItems.findStackInHotbar(ScaffoldFeature::isAllowedStack); }
        }
        if (holdingBlocks.getValue() && blockSlot == -1 && !ScaffoldFeature.isAllowedStack(mainHand)) { return; }
        canAssist = true;
        updateMovementAim(event, onGround && !event.jump);
        if (!onGround) { return; }
        if (sneakLatched) { event.sneak = true; }
        else if (requireSneak.getValue()) { event.sneak = false; }
    }

    public void onMovementPacket(MovementPacketEvent.Post event) {
        if (owner.isOutsideWorld() || !canAssist) { return; }
        boolean onGround = CLIENT.player.isOnGround();
        int level = onGround ? CLIENT.player.getSteppingPos().getY() : MathHelper.floor(CLIENT.player.getBoundingBox().minY) - 1;
        boolean descending = CLIENT.player.getVelocity().y < 0.0;
        bridgeY = onGround || increaseY.getValue() && descending ? level : Math.min(bridgeY, level);
        if (onGround) { lastSupport = CLIENT.player.getSteppingPos(); }
        target = findPlacement(CLIENT.player.getEntityPos(), CLIENT.player.getEyePos());
        if (target == null && autoRotate.getValue()) { predictedPitch = predictNextPitch(); }
    }

    /** Original AuditFactory identity is MouseMovementEvent.Post, not Pre. */
    public void onMouseMovement(MouseMovementEvent.Post event) {
        if (owner.isOutsideWorld()) { return; }
        RotationManager rotations = Echo.handlerManager.getRotationManager();
        if (!canAssist || !autoRotate.getValue() || MinecraftRenderState.getScreen() != null) {
            rotations.release(owner); return;
        }
        RotationSettings settings = new RotationSettings(RotationVisibility.SILENT, AimTransition.EASED, MIN_ROTATION_SPEED, MAX_ROTATION_SPEED);
        Vec3d point = aimPoint();
        if (point == null || !rotations.aim(owner, RotationPriority.NORMAL, point, settings)) { rotations.release(owner); }
    }

    public void onHotbarKey(HotbarKeyEvent event) {
        if (owner.isOutsideWorld() || blockSlot == -1 || event.slot != blockSlot) { return; }
        if (DeferredActionQueue.selectedSlot() == blockSlot) { return; }
        if (DeferredActionQueue.ensureSelected(owner, blockSlot, 0, false)) { event.addClick(); }
    }

    public void onUseKey(UseKeyEvent event) {
        if (owner.isOutsideWorld() || !autoPlace.getValue() || target == null || event.hasClicks()
                || MinecraftRenderState.getScreen() != null) { return; }
        ItemStack stack = CLIENT.player.getMainHandStack();
        if (!ScaffoldFeature.isAllowedStack(stack) || !PlacementWorldQueries.canPlaceStack(stack, target.hitResult())) { return; }
        RotationManager rotations = Echo.handlerManager.getRotationManager();
        if (!rotations.isRotationReady() || !BlockRaycasts.matchesHit(target.hitResult(), true)) { return; }
        if (CLIENT.player.age - lastLargeYawChangeTick < YAW_SETTLE_TICKS) { return; }
        event.addClick();
        rotations.acknowledgeRotation();
        lastSupport = target.position();
    }

    public void onBlockInteract(BlockInteractEvent.Post event) {
        if (owner.isOutsideWorld() || event.getPlayer() != CLIENT.player || event.getHand() != Hand.MAIN_HAND) { return; }
        lastPlacedTick = CLIENT.player.age;
    }

    private void trackYawChange() {
        float yaw = Echo.handlerManager.getRotationManager().effectiveRotation().yaw();
        if (lastLargeYawChangeTick > CLIENT.player.age) { lastLargeYawChangeTick = -1073741824; }
        if (!Float.isNaN(previousYaw) && Math.abs(MathHelper.wrapDegrees(yaw - previousYaw)) > LARGE_YAW_CHANGE) {
            lastLargeYawChangeTick = CLIENT.player.age;
        }
        previousYaw = yaw;
    }

    private void updateMovementAim(PlayerInputEvent event, boolean groundWithoutJump) {
        float strafe = PlayerInputEvent.movementMultiplier(event.left, event.right);
        float forward = PlayerInputEvent.movementMultiplier(event.forward, event.backward);
        if (strafe != 0.0F || forward != 0.0F) {
            inputYaw = CLIENT.player.getYaw() + (float) Math.toDegrees(MathHelper.atan2(-strafe, forward));
        } else if (Float.isNaN(inputYaw)) { inputYaw = CLIENT.player.getYaw(); }
        movementYaw = inputYaw;
        Vec3d velocity = CLIENT.player.getVelocity();
        if (!groundWithoutJump && velocity.horizontalLengthSquared() > MOTION_THRESHOLD * MOTION_THRESHOLD) {
            movementYaw = (float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z));
        }
        float remainder = Math.abs(MathHelper.wrapDegrees(movementYaw) % 90.0F);
        cardinalMovement = Math.min(remainder, 90.0F - remainder) <= 22.5F;
        boolean outsideColumn = false;
        if (cardinalMovement) {
            BlockPos position = targetPosition(CLIENT.player.getEntityPos());
            Direction sideways = Direction.fromHorizontalDegrees(movementYaw).rotateYCounterclockwise();
            Vec3d relative = CLIENT.player.getEntityPos().subtract(Vec3d.ofCenter(position));
            double lateral = relative.x * sideways.getOffsetX() + relative.z * sideways.getOffsetZ();
            outsideColumn = Math.abs(lateral) > 0.5;
            if ((groundWithoutJump || outsideColumn) && (Math.abs(lateral) > LATERAL_HYSTERESIS || diagonalOffset == 0.0F)) {
                diagonalOffset = lateral > 0.0 ? -45.0F : 45.0F;
            }
        }
        desiredYaw = avoidAlignedYaw(movementYaw + 180.0F
                + (cardinalMovement && (groundWithoutJump || outsideColumn) ? diagonalOffset : 0.0F));
    }

    private static float avoidAlignedYaw(float yaw) {
        yaw = MathHelper.wrapDegrees(yaw);
        float remainder = Math.abs(yaw % 45.0F);
        return Math.min(remainder, 45.0F - remainder) < ALIGNED_YAW_EPSILON ? MathHelper.wrapDegrees(yaw + YAW_NUDGE) : yaw;
    }

    private float predictNextPitch() {
        SimulatedPlayer simulation = PlayerPrediction.forWorld(CLIENT.world);
        Rotation rotation = Echo.handlerManager.getRotationManager().effectiveRotation();
        simulation.copyFrom(CLIENT.player, CLIENT.player.getVelocity(), rotation.yaw(), rotation.pitch(),
                CLIENT.player.forwardSpeed, CLIENT.player.sidewaysSpeed, CLIENT.player.input.playerInput.jump());
        simulation.tickPrediction();
        Vec3d eye = simulation.getEyePos();
        PlacementTarget predicted = findPlacement(simulation.getEntityPos(), eye);
        return predicted == null ? Float.NaN : Rotation.lookingAt(eye, predicted.hitResult().getPos()).pitch();
    }

    private BlockPos targetPosition(Vec3d position) {
        BlockPos target = BlockPos.ofFloored(position.x, bridgeY, position.z);
        if (lastSupport == null || !cardinalMovement) { return target; }
        Direction.Axis across = Direction.fromHorizontalDegrees(movementYaw).rotateYClockwise().getAxis();
        int column = lastSupport.getComponentAlongAxis(across);
        double halfWidth = CLIENT.player.getWidth() / 2.0;
        double component = position.getComponentAlongAxis(across);
        if (component + halfWidth <= column || component - halfWidth >= column + 1) { return target; }
        return target.offset(across, column - target.getComponentAlongAxis(across));
    }

    private PlacementTarget findPlacement(Vec3d position, Vec3d eye) {
        BlockPos target = targetPosition(position);
        if (!PlacementWorldQueries.isReplaceable(target)) { return null; }
        List<BlockPos> candidates = PlacementTargets.hasSupport(target, true) ? List.of(target) : nearbyTargets(target, position);
        for (BlockPos candidate : candidates) {
            BlockHitResult hit = searchPitch(eye, desiredYaw, candidate);
            if (hit == null) { hit = searchSideFaces(eye, candidate); }
            if (hit != null) { return new PlacementTarget(candidate, hit); }
        }
        return null;
    }

    private BlockHitResult searchSideFaces(Vec3d eye, BlockPos target) {
        Vec3d heading = Vec3d.fromPolar(0.0F, desiredYaw);
        float referencePitch = MathHelper.clamp(Echo.handlerManager.getRotationManager().effectiveRotation().pitch(), MIN_PITCH, MAX_PITCH);
        BlockHitResult best = null;
        float bestDifference = Float.MAX_VALUE;
        for (Direction side : Direction.Type.HORIZONTAL) {
            BlockPos neighbor = target.offset(side);
            Direction face = side.getOpposite();
            if (!PlacementTargets.supportsFace(neighbor, face, true)) { continue; }
            Direction.Axis axis = side.getAxis();
            Direction.Axis across = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
            double plane = target.getComponentAlongAxis(axis) + (side.getDirection() == Direction.AxisDirection.POSITIVE ? 1.0 : 0.0);
            double distance = (eye.getComponentAlongAxis(axis) - plane) * face.getDirection().offset();
            if (distance <= 0.0) { continue; }
            double lateral = eye.getComponentAlongAxis(across);
            double approach = -heading.getComponentAlongAxis(axis) * face.getDirection().offset();
            if (approach > 0.0) { lateral += distance / approach * heading.getComponentAlongAxis(across); }
            lateral = MathHelper.clamp(lateral, target.getComponentAlongAxis(across) + FACE_MARGIN,
                    target.getComponentAlongAxis(across) + 1.0 - FACE_MARGIN);
            for (double height = 0.9; height >= 0.1; height -= FACE_HEIGHT_STEP) {
                double y = neighbor.getY() + height;
                Vec3d point = axis == Direction.Axis.X ? new Vec3d(plane, y, lateral) : new Vec3d(lateral, y, plane);
                Rotation rotation = Rotation.lookingAt(eye, point);
                if (rotation.pitch() < MIN_PITCH || rotation.pitch() > MAX_PITCH) { continue; }
                BlockHitResult hit = BlockRaycasts.raycast(eye, avoidAlignedYaw(rotation.yaw()), rotation.pitch());
                if (hit == null || hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(neighbor) || hit.getSide() != face) { continue; }
                float difference = Math.abs(rotation.pitch() - referencePitch);
                if (difference < bestDifference) { bestDifference = difference; best = hit; }
                break;
            }
        }
        return best;
    }

    private List<BlockPos> nearbyTargets(BlockPos target, Vec3d position) {
        return Direction.Type.HORIZONTAL.stream().map(target::offset)
                .filter(candidate -> PlacementWorldQueries.isReplaceable(candidate) && PlacementTargets.hasSupport(candidate, true))
                .sorted(Comparator.comparingDouble(candidate -> candidate.getSquaredDistance(position))).toList();
    }

    private BlockHitResult searchPitch(Vec3d eye, float yaw, BlockPos target) {
        float reference = MathHelper.clamp(Echo.handlerManager.getRotationManager().effectiveRotation().pitch(), MIN_PITCH, MAX_PITCH);
        for (float offset = 0.0F; offset <= MAX_PITCH - MIN_PITCH; offset += PITCH_STEP) {
            for (float pitch : new float[] {reference + offset, reference - offset}) {
                if (pitch < MIN_PITCH || pitch > MAX_PITCH) { continue; }
                BlockHitResult hit = BlockRaycasts.raycast(eye, yaw, pitch);
                if (hit == null || hit.getType() != HitResult.Type.BLOCK) { continue; }
                BlockPos support = hit.getBlockPos();
                if (PlacementTargets.supportsFace(support, hit.getSide(), true) && support.offset(hit.getSide()).equals(target)) { return hit; }
            }
        }
        return null;
    }

    private Vec3d aimPoint() {
        Vec3d eye = CLIENT.player.getEyePos();
        if (target != null) {
            Vec3d point = target.hitResult().getPos();
            retainedPitch = Rotation.lookingAt(eye, point).pitch();
            return point;
        }
        if (!Float.isNaN(predictedPitch)) { retainedPitch = predictedPitch; }
        if (Float.isNaN(retainedPitch)) {
            BlockPos position = targetPosition(CLIENT.player.getEntityPos());
            if (PlacementWorldQueries.isReplaceable(position)) { return null; }
            Vec3d edge = Vec3d.ofCenter(position).offset(Direction.fromHorizontalDegrees(movementYaw), 0.5);
            retainedPitch = Rotation.lookingAt(eye, edge).pitch();
        }
        return eye.add(Vec3d.fromPolar(retainedPitch, desiredYaw).multiply(2.0));
    }

    private boolean needsSneak(PlayerInputEvent event) {
        Vec3d motion = PlayerPrediction.predictDisplacement(CLIENT.player,
                PlayerInputEvent.movementMultiplier(event.forward, event.backward), PlayerInputEvent.movementMultiplier(event.left, event.right));
        if (canFallAtOffset(motion.x, motion.z)) { return true; }
        double radius = edgeOffset.getValue();
        for (int sample = 0; sample < 8; sample++) {
            double angle = sample * 0.7853981633974483;
            if (canFallAtOffset(motion.x + Math.cos(angle) * radius, motion.z + Math.sin(angle) * radius)) { return true; }
        }
        return false;
    }

    private boolean canFallAtOffset(double offsetX, double offsetZ) {
        return ((PlayerEntityAccessor) CLIENT.player).invokeIsSpaceAroundPlayerEmpty(offsetX, offsetZ, CLIENT.player.getStepHeight());
    }

    public static void setInputMarker(Event[] marker) { inputMarker = marker; }
    public static Event[] getInputMarker() { return inputMarker; }
}
