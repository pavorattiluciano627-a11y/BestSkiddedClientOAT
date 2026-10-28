package dev.echo.client.features.impl.utility;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HotbarKeyEvent;
import dev.echo.client.events.input.MouseMovementEvent;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.events.input.UseKeyEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.handlers.AimTransition;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.handlers.RotationPriority;
import dev.echo.client.handlers.RotationSettings;
import dev.echo.client.handlers.RotationVisibility;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.setting.MultiChoiceSetting;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.targeting.BlockRaycasts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ordered hotbar fall-breaking, including side-supported ladder/vine placement.
 * Source candidate in the recorded normal control/caller domain; applicability
 * and the native disable field join are documented in the equipment receipt.
 */
public class MLGFeature extends Feature {
    private static final float MAX_ROTATION_STEP = 250.0F;
    private static final int SNEAK_LOOKAHEAD_TICKS = 5;
    private static final Item[] fallBreakingItems = {
            Items.WATER_BUCKET, Items.LADDER, Items.VINE,
            Items.SLIME_BLOCK, Items.HAY_BLOCK, Items.HONEY_BLOCK
    };
    private static final CharSequence[] itemsOptionSet = {
            CatalogText.literal("Water Bucket"), CatalogText.literal("Ladder"),
            CatalogText.literal("Vine"), CatalogText.literal("Slime Block"),
            CatalogText.literal("Hay Bale"), CatalogText.literal("Honey Block")
    };
    private static IntSetting fallDistanceThreshold;
    private static IntSetting distanceToGround;
    private static MultiChoiceSetting items;
    private static BooleanSetting autoAim;
    private static BooleanSetting silent;
    private BlockHitResult placementHit;
    private Item placementItem;
    private int placementSlot;
    private boolean disablePending;

    public MLGFeature() {
        super(new dev.echo.client.features.FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("MLG"), dev.echo.client.bootstrap.CatalogText.literal("Uses hotbar items to break your fall"), dev.echo.client.features.Category.UTILITY));
    }

    @Override
    public void postInitialize() {
        silent.setVisibilityPredicate(ignored -> autoAim.getValue());
        items.setSelected(CatalogText.literal("Water Bucket"), true);
        distanceToGround.setAllowOutOfRange(true);
        fallDistanceThreshold.setAllowOutOfRange(true);
    }

    @Override
    public void onEnable() {
        placementHit = null;
        disablePending = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        placementHit = null;
        DeferredActionQueue.finish(this, true);
        if (silent.getValue() && rotations().isOwnedBy(this)) {
            disablePending = true;
            return;
        }
        disablePending = false;
        rotations().cancel(this);
        super.onDisable();
    }

    @Subscribe
    private void afterMouseMovement(MouseMovementEvent.Post event) {
        if (disablePending) {
            finishPendingDisable();
            return;
        }
        if (isOutsideWorld() || currentScreen() != null) {
            placementHit = null;
            releasePlacement();
            return;
        }
        placementHit = findLandingHit();
        if (placementHit == null) {
            releasePlacement();
            return;
        }
        if (!autoAim.getValue()) {
            return;
        }
        if (!rotations().aim(this, RotationPriority.HIGH, adjustPlacementPoint(placementHit),
                RotationSettings.uniform(silent.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE,
                        AimTransition.EASED, MAX_ROTATION_STEP))) {
            releasePlacement();
        }
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (placementHit == null || isOutsideWorld() || event.slot != placementSlot) {
            return;
        }
        if (DeferredActionQueue.selectedSlot() != placementSlot
                && DeferredActionQueue.ensureSelected(this, placementSlot, -1, true)) {
            event.addClick();
        }
    }

    @Subscribe
    private void onUseKey(UseKeyEvent event) {
        if (placementHit == null || isOutsideWorld() || currentScreen() != null) {
            return;
        }
        if (!client.player.getMainHandItem().is(placementItem)) {
            return;
        }
        if (!BlockRaycasts.matchesHit(placementHit, true)) {
            return;
        }
        if (!client.player.isShiftKeyDown()) {
            return;
        }
        event.addClick();
    }

    @Subscribe
    private void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld() || placementHit == null) {
            return;
        }
        double fallingSpeed = Math.max(0.1, -client.player.getDeltaMovement().y);
        if (client.player.getY() - placementHit.getLocation().y <= fallingSpeed * SNEAK_LOOKAHEAD_TICKS) {
            event.sneak = true;
        }
    }

    private BlockHitResult findLandingHit() {
        if (client.player.onGround() || client.player.isInWater() || client.player.getDeltaMovement().y >= 0.0) {
            return null;
        }
        AABB bounds = client.player.getBoundingBox().deflate(0.001, 0.0, 0.001);
        Vec3 center = bounds.getCenter();
        double rayDepth = Math.max(distanceToGround.getValue(), bounds.minY - client.level.getMinY());
        BlockHitResult highestHit = null;
        double[][] origins = {
                {center.x, center.z}, {bounds.minX, bounds.minZ}, {bounds.minX, bounds.maxZ},
                {bounds.maxX, bounds.minZ}, {bounds.maxX, bounds.maxZ}
        };
        for (double[] origin : origins) {
            Vec3 from = new Vec3(origin[0], bounds.minY, origin[1]);
            Vec3 to = from.subtract(0.0, rayDepth, 0.0);
            BlockHitResult hit = client.level.clip(new ClipContext(from, to,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, client.player));
            if (hit.getType() != HitResult.Type.BLOCK) {
                continue;
            }
            if (highestHit == null || hit.getLocation().y > highestHit.getLocation().y) {
                highestHit = hit;
            }
        }
        if (highestHit == null) {
            return findSideSupportedHit();
        }
        if (client.player.fallDistance < fallDistanceThreshold.getValue()) {
            return null;
        }
        if (bounds.minY - highestHit.getLocation().y > distanceToGround.getValue()) {
            return null;
        }
        if (alreadyBreaksFall(highestHit.getBlockPos())) {
            return null;
        }
        if (resolvePlacementItem(highestHit.getBlockPos())) {
            return highestHit;
        }
        return findSideSupportedHit();
    }

    private BlockHitResult findSideSupportedHit() {
        if (!resolvePlacementItem(null)) {
            return null;
        }
        for (int depth = 1; depth <= distanceToGround.getValue(); depth++) {
            BlockPos position = client.player.blockPosition().below(depth);
            if (!client.level.getBlockState(position).canBeReplaced()) {
                return null;
            }
            Direction support = findHorizontalSupport(position);
            if (support != null) {
                return new BlockHitResult(Vec3.atCenterOf(position).relative(support, 0.5),
                        support.getOpposite(), position.relative(support), false);
            }
        }
        return null;
    }

    private boolean alreadyBreaksFall(BlockPos position) {
        BlockState state = client.level.getBlockState(position);
        return !state.getFluidState().isEmpty()
                || state.is(Blocks.SLIME_BLOCK)
                || state.is(Blocks.HAY_BLOCK)
                || state.is(Blocks.HONEY_BLOCK)
                || client.level.getBlockState(position.above()).is(BlockTags.CLIMBABLE);
    }

    private boolean resolvePlacementItem(BlockPos landingBlock) {
        for (int index = 0; index < fallBreakingItems.length; index++) {
            Item item = fallBreakingItems[index];
            boolean requiresSideSupport = item == Items.LADDER || item == Items.VINE;
            if (landingBlock == null) {
                if (!requiresSideSupport) {
                    continue;
                }
            } else if (requiresSideSupport && findHorizontalSupport(landingBlock.above()) == null) {
                continue;
            }
            if (!items.isSelected(itemsOptionSet[index])) {
                continue;
            }
            int slot = InventoryItems.findStackInHotbar(stack -> matchesItem(item, stack));
            if (slot == -1) {
                continue;
            }
            placementItem = item;
            placementSlot = slot;
            return true;
        }
        return false;
    }

    private Direction findHorizontalSupport(BlockPos position) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = position.relative(direction);
            if (client.level.getBlockState(neighbor).isFaceSturdy(client.level, neighbor, direction.getOpposite())) {
                return direction;
            }
        }
        return null;
    }

    private Vec3 adjustPlacementPoint(BlockHitResult hit) {
        Vec3 point = hit.getLocation();
        if (hit.getDirection() != Direction.UP) {
            return point;
        }
        Vec3 forward = client.player.calculateViewVector(0.0F, client.player.getYRot());
        BlockPos block = hit.getBlockPos();
        double x = Mth.clamp(point.x + forward.x * 0.3, block.getX() + 0.05, block.getX() + 0.95);
        double z = Mth.clamp(point.z + forward.z * 0.3, block.getZ() + 0.05, block.getZ() + 0.95);
        return new Vec3(x, point.y, z);
    }

    private void releasePlacement() {
        DeferredActionQueue.finish(this, true);
        if (silent.getValue()) {
            rotations().beginReturn(this);
        } else {
            rotations().cancel(this);
        }
    }

    private void finishPendingDisable() {
        if (rotations().beginReturn(this)) {
            return;
        }
        disablePending = false;
        super.onDisable();
    }

    private static boolean matchesItem(Item item, ItemStack stack) {
        return stack.is(item);
    }

    private static Screen currentScreen() {
        try {
            Class.forName(Screen.class.getName(), true, Screen.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        return Minecraft.getInstance().gui.screen();
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(MLGFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, MLGFeature.fallDistanceThreshold);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, MLGFeature.distanceToGround);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, MLGFeature.items);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, MLGFeature.autoAim);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, MLGFeature.silent);
    }

    /** Reads the shared itemsOptionSet default without copying its value. */
    public static java.lang.CharSequence[] bootstrapItemsOptionSet() {
        return itemsOptionSet;
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(MLGFeature feature, dev.echo.client.setting.IntSetting preparedDistanceToGround, dev.echo.client.setting.MultiChoiceSetting preparedItems, dev.echo.client.setting.IntSetting preparedFallDistanceThreshold, dev.echo.client.setting.BooleanSetting preparedAutoAim, dev.echo.client.setting.BooleanSetting preparedSilent) {
        MLGFeature.distanceToGround = preparedDistanceToGround;
        MLGFeature.items = preparedItems;
        MLGFeature.fallDistanceThreshold = preparedFallDistanceThreshold;
        MLGFeature.autoAim = preparedAutoAim;
        MLGFeature.silent = preparedSilent;
    }
}
