package dev.echo.client.features.impl.combat;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HotbarKeyEvent;
import dev.echo.client.events.input.MouseMovementEvent;
import dev.echo.client.events.input.UseKeyEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.handlers.AimTransition;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.handlers.RotationManager;
import dev.echo.client.handlers.RotationPriority;
import dev.echo.client.handlers.RotationSettings;
import dev.echo.client.handlers.RotationVisibility;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.KeybindSetting;
import dev.echo.client.setting.MultiChoiceSetting;
import dev.echo.client.utils.Rotation;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.placement.BlockPlacement;
import dev.echo.client.utils.placement.PlacementTarget;
import dev.echo.client.utils.targeting.BlockAimRotation;
import dev.echo.client.utils.targeting.BlockRaycasts;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Rail/cart/fire/crossbow sequence. Original null-reader, call-binding and nested
 * provider domains remain explicit in the receipt; NB-069 is still conditional.
 */
public class XBowFeature extends Feature {
    private static final float AIM_SPEED = 350.0F;
    private static final int TIMEOUT_TICKS = 40;
    private static final double FIRE_MARGIN = 0.2;
    private static final double SEARCH_STEP = 0.1;
    private static final double CART_INSET = 0.05;
    private static final int FIRE = 0;
    private static final int RAIL = 1;
    private static final int CART = 2;
    private static final int SHOOT = 3;
    private static final CharSequence placeOption2 = CatalogText.literal("Fire");
    private static final CharSequence placeOption4 = CatalogText.literal("Rail");
    private static final CharSequence placeOption3 = CatalogText.literal("Cart");
    private static final CharSequence placeOption = CatalogText.literal("Shoot");
    private static KeybindSetting activateKey;
    private static MultiChoiceSetting place;
    private static BooleanSetting autoAim;
    private static BooleanSetting silentRotations;
    private BlockPos targetBlockPosition;
    private BlockPos supportBlockPosition;
    private Vec3 cachedTargetPosition;
    private Vec3 targetHitPosition;
    private long activationTick;
    private BlockHitResult pendingBlockHit;
    private int placementStep;
    private int selectedPlacementSlot;
    private boolean interactionSubmitted;
    private boolean activationKeyHeld;
    private boolean waitingForRotationRelease;

    public XBowFeature() {
        super(new FeatureInfo(CatalogText.literal("XBow"),
                CatalogText.literal("Lights a fire, lays a TNT cart and shoots your crossbow through it"),
                Category.COMBAT));
    }

    public static void appendBootstrapSettings(XBowFeature feature) {
        CatalogSettings.append(feature, activateKey);
        CatalogSettings.append(feature, place);
        CatalogSettings.append(feature, autoAim);
        CatalogSettings.append(feature, silentRotations);
    }

    public static CharSequence bootstrapPlaceOption2() { return placeOption2; }
    public static CharSequence bootstrapPlaceOption4() { return placeOption4; }
    public static CharSequence bootstrapPlaceOption3() { return placeOption3; }
    public static CharSequence bootstrapPlaceOption() { return placeOption; }

    public static void assignBootstrapSettings(XBowFeature feature, MultiChoiceSetting preparedPlace,
            KeybindSetting preparedActivateKey, BooleanSetting preparedAutoAim,
            BooleanSetting preparedSilentRotations) {
        place = preparedPlace;
        activateKey = preparedActivateKey;
        autoAim = preparedAutoAim;
        silentRotations = preparedSilentRotations;
    }

    @Override
    public void postInitialize() {
        place.setSelected(placeOption2, true);
        place.setSelected(placeOption4, true);
        place.setSelected(placeOption3, true);
        place.setSelected(placeOption, true);
        silentRotations.setVisibilityPredicate(ignored -> autoAim.getValue());
    }

    /** Accepted NB-068 body, including its original initialization boundaries. */
    @Override
    public void onEnable() {
        try {
            Class.forName(BlockPos.class.getName(), true, BlockPos.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        targetBlockPosition = null;
        supportBlockPosition = null;
        try {
            Class.forName(Vec3.class.getName(), true, Vec3.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        cachedTargetPosition = null;
        targetHitPosition = null;
        pendingBlockHit = null;
        selectedPlacementSlot = -1;
        interactionSubmitted = false;
        activationKeyHeld = false;
        waitingForRotationRelease = false;
        super.onEnable();
    }

    /** NB-069 null-reader branch. Ownership and return-in-progress are distinct. */
    @Override
    public void onDisable() {
        clearPlacementState();
        DeferredActionQueue.finish(this, true);
        try {
            Class.forName(BooleanSetting.class.getName(), true, BooleanSetting.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        if (silentRotations.getValue()) {
            try {
                Class.forName(RotationManager.class.getName(), true, RotationManager.class.getClassLoader());
            } catch (ClassNotFoundException failure) {
                throw new IllegalStateException("Resolved class initialization failed", failure);
            }
            if (rotations().isOwnedBy(this)) {
                waitingForRotationRelease = true;
                return;
            }
        }
        waitingForRotationRelease = false;
        try {
            Class.forName(RotationManager.class.getName(), true, RotationManager.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        rotations().cancel(this);
        super.onDisable();
    }

    @Subscribe
    private void onUseKey(UseKeyEvent event) {
        boolean pressed = activateKey.getKey().isPressed();
        boolean risingEdge = pressed && !activationKeyHeld;
        activationKeyHeld = pressed;
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) return;
        if (targetBlockPosition == null) {
            if (risingEdge) beginSequence();
            return;
        }
        if (placementStep == SHOOT) {
            if (selectedPlacementSlot == -1) {
                if (!InventoryItems.isChargedCrossbow(client.player.getOffhandItem())) return;
            } else if (DeferredActionQueue.selectedSlot() != selectedPlacementSlot) {
                return;
            }
            if (!canShootThroughFire()) return;
            event.addClick();
            finishSequence();
            return;
        }
        if (pendingBlockHit == null || DeferredActionQueue.selectedSlot() != selectedPlacementSlot
                || !BlockRaycasts.matchesHit(pendingBlockHit, placementStep != CART)) return;
        if (placementStep == CART) {
            if (interactionSubmitted) return;
            interactionSubmitted = true;
        }
        event.addClick();
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (targetBlockPosition == null || isOutsideWorld() || selectedPlacementSlot == -1
                || event.slot != selectedPlacementSlot) return;
        if (DeferredActionQueue.selectedSlot() != selectedPlacementSlot
                && DeferredActionQueue.ensureSelected(this, selectedPlacementSlot, -1, true)) {
            event.addClick();
        }
    }

    @Subscribe
    private void afterMouseMovement(MouseMovementEvent.Post event) {
        if (waitingForRotationRelease) {
            finishDisableAfterReturn();
            return;
        }
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            if (targetBlockPosition != null) abortSequence();
            else releaseRotation();
            return;
        }
        if (targetBlockPosition == null) {
            releaseRotation();
            return;
        }
        if ((long) client.player.tickCount - activationTick > TIMEOUT_TICKS) {
            abortSequence();
            return;
        }
        if (!prepareStep()) return;
        if (autoAim.getValue() && !rotations().aim(this, RotationPriority.HIGH,
                placementStep == SHOOT ? targetHitPosition : pendingBlockHit.getLocation(),
                RotationSettings.uniform(silentRotations.getValue()
                        ? RotationVisibility.SILENT : RotationVisibility.VISIBLE,
                        AimTransition.EASED, AIM_SPEED))) {
            abortSequence();
        }
    }

    private void beginSequence() {
        if (!(place.isSelected(placeOption2) || place.isSelected(placeOption4)
                || place.isSelected(placeOption3) || place.isSelected(placeOption))) return;
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos target = BaseRailBlock.isRail(client.level, hit.getBlockPos())
                ? hit.getBlockPos() : BlockPlacement.placementPosition(hit);
        Vec3 hitPoint = hit.getLocation();
        Vec3 eye = client.player.getEyePosition(1.0F);
        double reachSquared = Mth.square(client.player.blockInteractionRange());
        boolean railPresent = BaseRailBlock.isRail(client.level, target);
        if (place.isSelected(placeOption4) && !railPresent) {
            if (!BlockPlacement.isReplaceable(target) || !Block.canSupportRigidBlock(client.level, target.below())) return;
            if (findStepSlot(RAIL) == -1) return;
            BlockHitResult railHit = BlockAimRotation.findHit(target, hitPoint);
            if (railHit == null || eye.distanceToSqr(railHit.getLocation()) > reachSquared) return;
        }
        if (place.isSelected(placeOption3)) {
            if (!railPresent && !place.isSelected(placeOption4)) return;
            if (findStepSlot(CART) == -1) return;
            if (eye.distanceToSqr(BlockAimRotation.insetBottomPoint(target, hitPoint)) > reachSquared) return;
        }
        if (place.isSelected(placeOption) && !hasCrossbow(findStepSlot(SHOOT))) return;
        supportBlockPosition = null;
        targetHitPosition = Vec3.atLowerCornerWithOffset(target, 0.5, 0.35, 0.5);
        if (place.isSelected(placeOption2)) {
            if (findStepSlot(FIRE) == -1) return;
            if (!findAdjacentFirePosition(target, eye, reachSquared)
                    && !findFirePositionAlongShot(target, eye, reachSquared)) return;
        }
        targetBlockPosition = target;
        cachedTargetPosition = hitPoint;
        activationTick = client.player.tickCount;
    }

    private boolean findAdjacentFirePosition(BlockPos target, Vec3 eye, double reachSquared) {
        Vec3 offset = eye.subtract(Vec3.atCenterOf(target));
        Direction bestDirection = null;
        double bestAlignment = 0.5;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            double alignment = direction.getStepX() * offset.x + direction.getStepZ() * offset.z;
            if (alignment <= bestAlignment) continue;
            BlockPos candidate = target.relative(direction);
            if (!BaseFireBlock.canBePlacedAt(client.level, candidate, client.player.getDirection())) continue;
            if (client.player.getBoundingBox().intersects(new AABB(candidate))) continue;
            PlacementTarget placement = BlockPlacement.find(candidate, true);
            if (placement == null || eye.distanceToSqr(placement.hitResult().getLocation()) > reachSquared) continue;
            bestDirection = direction;
            bestAlignment = alignment;
        }
        if (bestDirection == null) return false;
        supportBlockPosition = target.relative(bestDirection);
        targetHitPosition = Vec3.atLowerCornerWithOffset(target, 0.5 + bestDirection.getStepX() * 0.3,
                0.35, 0.5 + bestDirection.getStepZ() * 0.3);
        return true;
    }

    private boolean findFirePositionAlongShot(BlockPos target, Vec3 eye, double reachSquared) {
        Vec3 delta = targetHitPosition.subtract(eye);
        double distance = delta.length();
        Vec3 direction = delta.scale(1.0 / distance);
        Set<BlockPos> visited = new HashSet<>();
        for (double along = distance; along > 0.0; along -= SEARCH_STEP) {
            BlockPos candidate = BlockPos.containing(eye.add(direction.scale(along)));
            if (tryFirePosition(candidate, target, eye, reachSquared, visited)
                    || tryFirePosition(candidate.below(), target, eye, reachSquared, visited)) return true;
        }
        return false;
    }

    private boolean tryFirePosition(BlockPos candidate, BlockPos target, Vec3 eye,
            double reachSquared, Set<BlockPos> visited) {
        if (candidate.equals(target) || !visited.add(candidate)) return false;
        AABB bounds = new AABB(candidate).inflate(FIRE_MARGIN);
        if (!bounds.contains(eye) && bounds.clip(eye, targetHitPosition).isEmpty()) return false;
        if (!BaseFireBlock.canBePlacedAt(client.level, candidate, client.player.getDirection())) return false;
        PlacementTarget placement = BlockPlacement.find(candidate, true);
        if (placement == null || eye.distanceToSqr(placement.hitResult().getLocation()) > reachSquared) return false;
        supportBlockPosition = candidate;
        return true;
    }

    private boolean prepareStep() {
        placementStep = nextStep();
        if (placementStep == -1) {
            abortSequence();
            return false;
        }
        if (placementStep != CART) interactionSubmitted = false;
        pendingBlockHit = null;
        selectedPlacementSlot = findStepSlot(placementStep);
        boolean awaitingCart = placementStep == CART && selectedPlacementSlot == -1 && interactionSubmitted;
        if (!awaitingCart && (placementStep == SHOOT
                ? !hasCrossbow(selectedPlacementSlot) : selectedPlacementSlot == -1)) {
            abortSequence();
            return false;
        }
        if (placementStep == SHOOT) return true;
        pendingBlockHit = findStepHit();
        return pendingBlockHit != null;
    }

    private static int findStepSlot(int step) {
        return switch (step) {
            case FIRE -> InventoryItems.findStackInHotbar(XBowFeature::isIgnitionItem);
            case RAIL -> InventoryItems.findStackInHotbar(InventoryItems::isRail);
            case CART -> InventoryItems.findStackInHotbar(XBowFeature::isTntCart);
            default -> InventoryItems.findStackInHotbar(InventoryItems::isChargedCrossbow);
        };
    }

    private static boolean isIgnitionItem(ItemStack stack) { return stack.is(Items.FLINT_AND_STEEL); }
    private static boolean isTntCart(ItemStack stack) { return stack.is(Items.TNT_MINECART); }

    private static boolean hasCrossbow(int slot) {
        return slot != -1 || InventoryItems.isChargedCrossbow(client.player.getOffhandItem());
    }

    private int nextStep() {
        boolean railPresent = BaseRailBlock.isRail(client.level, targetBlockPosition);
        if (place.isSelected(placeOption4) && !railPresent) return RAIL;
        boolean cartPresent = findCart(targetBlockPosition) != null;
        if (place.isSelected(placeOption3) && railPresent && !cartPresent) return CART;
        if (place.isSelected(placeOption2) && !client.level.getBlockState(supportBlockPosition).is(BlockTags.FIRE)) return FIRE;
        return place.isSelected(placeOption) && cartPresent ? SHOOT : -1;
    }

    private BlockHitResult findStepHit() {
        Vec3 eye = client.player.getEyePosition(1.0F);
        double reachSquared = Mth.square(client.player.blockInteractionRange());
        if (placementStep == FIRE) {
            if (!BaseFireBlock.canBePlacedAt(client.level, supportBlockPosition, client.player.getDirection())) return null;
            PlacementTarget placement = BlockPlacement.find(supportBlockPosition, true);
            return placement != null && eye.distanceToSqr(placement.hitResult().getLocation()) <= reachSquared
                    ? placement.hitResult() : null;
        }
        if (placementStep == RAIL) {
            if (!BlockPlacement.isReplaceable(targetBlockPosition)
                    || !Block.canSupportRigidBlock(client.level, targetBlockPosition.below())) return null;
            BlockHitResult hit = BlockAimRotation.findHit(targetBlockPosition, cachedTargetPosition);
            return hit != null && eye.distanceToSqr(hit.getLocation()) <= reachSquared ? hit : null;
        }
        BlockHitResult hit = new BlockHitResult(BlockAimRotation.insetBottomPoint(targetBlockPosition,
                cachedTargetPosition), Direction.UP, targetBlockPosition, false);
        return eye.distanceToSqr(hit.getLocation()) <= reachSquared ? hit : null;
    }

    private boolean canShootThroughFire() {
        Vec3 start = client.player.getEyePosition(1.0F).subtract(0.0, 0.1, 0.0);
        Rotation rotation = rotations().effectiveRotation();
        Vec3 direction = Vec3.directionFromRotation(rotation.pitch(), rotation.yaw());
        Vec3 end = start.add(direction.scale(client.player.blockInteractionRange() + 2.0));
        if (place.isSelected(placeOption2)) {
            AABB fireBounds = new AABB(supportBlockPosition).inflate(FIRE_MARGIN);
            if (!fireBounds.contains(start) && fireBounds.clip(start, end).isEmpty()) return false;
        }
        MinecartTNT cart = findCart(targetBlockPosition);
        return cart != null && cart.getBoundingBox().deflate(CART_INSET).clip(start, end).isPresent();
    }

    private MinecartTNT findCart(BlockPos position) {
        List<MinecartTNT> carts = client.level.getEntitiesOfClass(MinecartTNT.class, new AABB(position));
        return carts.isEmpty() ? null : carts.getFirst();
    }

    private void finishSequence() {
        DeferredActionQueue.select(this, DeferredActionQueue.selectedSlot(), 1);
        clearPlacementState();
        releaseRotation();
    }

    private void abortSequence() {
        clearPlacementState();
        DeferredActionQueue.finish(this, true);
        releaseRotation();
    }

    private void clearPlacementState() {
        try {
            Class.forName(BlockPos.class.getName(), true, BlockPos.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        targetBlockPosition = null;
        try {
            Class.forName(BlockPos.class.getName(), true, BlockPos.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        supportBlockPosition = null;
        try {
            Class.forName(Vec3.class.getName(), true, Vec3.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        cachedTargetPosition = null;
        try {
            Class.forName(Vec3.class.getName(), true, Vec3.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        targetHitPosition = null;
        pendingBlockHit = null;
        selectedPlacementSlot = -1;
        interactionSubmitted = false;
    }

    private void releaseRotation() {
        if (silentRotations.getValue()) rotations().beginReturn(this);
        else rotations().cancel(this);
    }

    private void finishDisableAfterReturn() {
        if (rotations().beginReturn(this)) return;
        waitingForRotationRelease = false;
        super.onDisable();
    }
}
