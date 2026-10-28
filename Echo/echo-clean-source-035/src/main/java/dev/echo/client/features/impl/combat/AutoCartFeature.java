package dev.echo.client.features.impl.combat;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HotbarKeyEvent;
import dev.echo.client.events.input.MouseMovementEvent;
import dev.echo.client.events.input.UseKeyEvent;
import dev.echo.client.events.network.PacketSendEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.handlers.AimTransition;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.handlers.LatencyTracker;
import dev.echo.client.handlers.RotationPriority;
import dev.echo.client.handlers.RotationSettings;
import dev.echo.client.handlers.RotationVisibility;
import dev.echo.client.mixin.PersistentProjectileEntityAccessor;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.HotbarSlotsSetting;
import dev.echo.client.setting.ItemListSetting;
import dev.echo.client.setting.MultiChoiceSetting;
import dev.echo.client.utils.Rotation;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.items.ItemProperties;
import dev.echo.client.utils.placement.PlacementTarget;
import dev.echo.client.utils.placement.PlacementTargets;
import dev.echo.client.utils.placement.PlacementWorldQueries;
import dev.echo.client.utils.targeting.BlockAimRotation;
import dev.echo.client.utils.targeting.BlockRaycasts;
import dev.echo.client.utils.trajectory.BlockImpactProjectileSimulation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** Predicts flaming-arrow impacts and sequences protective blocks, rails and TNT carts. */
public class AutoCartFeature extends Feature {
    private static final float ROTATION_SPEED = 200.0f;
    private static final int MAX_SIMULATION_TICKS = 300;
    private static final int FRESH_ARROW_AGE = 3;
    private static final int LATENCY_MARGIN_TICKS = 5;
    private static final int PREDICTED_SHOT_LEAD_TICKS = 2;
    private static final int RELEASE_LOOKAHEAD_TICKS = 5;
    private static final int PLACE_RAIL = 0;
    private static final int PLACE_CART = 1;
    private static final int PLACE_PROTECTION = 2;
    private static final CharSequence placeOption = CatalogText.literal("Rail");
    private static final CharSequence placeOption2 = CatalogText.literal("Cart");
    private static final CharSequence placeOption3 = CatalogText.literal("Protect");
    private static final CharSequence returnToOption3 = CatalogText.literal("Bow");
    private static final CharSequence returnToOption2 = CatalogText.literal("Slot");
    private static final CharSequence returnToOption = CatalogText.literal("Item");
    private static MultiChoiceSetting place;
    private static BooleanSetting silentRotations;
    private static BooleanSetting freshArrowsOnly;
    private static ChoiceSetting returnTo;
    private static HotbarSlotsSetting returnSlots;
    private static ItemListSetting returnItems;
    private Set<Integer> trackedProjectileIds;
    private PersistentProjectileEntity trackedProjectile;
    private PersistentProjectileEntity lastSeenProjectile;
    private BlockPos railPosition;
    private BlockPos supportBlockPosition;
    private Vec3d targetHitPosition;
    private BlockHitResult pendingBlockHit;
    private int placementStep;
    private int selectedPlacementSlot;
    private boolean waitingForProjectile;
    private boolean interactionSubmitted;
    private int projectileWaitUntilTick;
    private Vec3d lastProjectilePosition;
    private Vec3d lastProjectileVelocity;
    private boolean disablePending;

    public AutoCartFeature() {
        super(new dev.echo.client.features.FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Auto Cart"), dev.echo.client.bootstrap.CatalogText.literal("Lands your flame arrow on a freshly placed TNT cart"), dev.echo.client.features.Category.COMBAT));
    }

    @Override
    public void postInitialize() {
        trackedProjectileIds = new HashSet<>();
        projectileWaitUntilTick = -1;
        place.setSelected(placeOption, true);
        place.setSelected(placeOption2, true);
        returnSlots.setVisibilityPredicate(ignored -> returnTo.isSelected(returnToOption2));
        returnItems.setVisibilityPredicate(ignored -> returnTo.isSelected(returnToOption));
    }

    /** NB-044: initialization and every reset stay in the reviewed failure order. */
    @Override
    public void onEnable() {
        try {
            Class.forName(PersistentProjectileEntity.class.getName(), true,
                    PersistentProjectileEntity.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        trackedProjectile = null;
        try {
            Class.forName(BlockPos.class.getName(), true, BlockPos.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        railPosition = null;
        supportBlockPosition = null;
        try {
            Class.forName(Vec3d.class.getName(), true, Vec3d.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        targetHitPosition = null;
        pendingBlockHit = null;
        selectedPlacementSlot = -1;
        interactionSubmitted = false;
        projectileWaitUntilTick = -1;
        lastProjectilePosition = null;
        lastProjectileVelocity = null;
        Objects.requireNonNull(trackedProjectileIds, "object reference is null").clear();
        lastSeenProjectile = null;
        disablePending = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        String[] controlState = SpearReachFeature.readSharedControlState();
        resetSequence();
        if (controlState != null) return;
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
    private void onMouseMovement(MouseMovementEvent.Post event) {
        String[] controlState = SpearReachFeature.readSharedControlState();
        if (controlState != null) {
            releaseRotation();
            return;
        }
        if (disablePending) {
            completeDisable();
            return;
        }
        if (isOutsideWorld() || MinecraftClient.getInstance().currentScreen != null) {
            if (isSequenceActive()) finishSequence(false);
            else releaseRotation();
            return;
        }
        scanProjectiles();
        if (!isSequenceActive()) {
            releaseRotation();
            return;
        }
        if (trackedProjectile != null) {
            if (trackedProjectile.isRemoved()
                    || ((PersistentProjectileEntityAccessor) trackedProjectile).invokeIsInGround()) {
                finishSequence(false);
                return;
            }
        } else if (client.player.age > projectileWaitUntilTick) {
            finishSequence(false);
            return;
        }
        if (!updatePlacementStep()) return;
        var rotationManager = rotations();
        Vec3d aimPoint = pendingBlockHit.getPos();
        RotationSettings settings = silentRotations.getValue()
                ? RotationSettings.uniform(RotationVisibility.SILENT, AimTransition.SNAP, ROTATION_SPEED)
                : RotationSettings.uniform(RotationVisibility.VISIBLE, AimTransition.EASED, ROTATION_SPEED);
        if (!rotationManager.aim(this, RotationPriority.HIGH, aimPoint, settings)) finishSequence(false);
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        String[] controlState = SpearReachFeature.readSharedControlState();
        int requestedSlot;
        if (controlState == null) {
            if (!isSequenceActive()) return;
            if (isOutsideWorld()) return;
            if (selectedPlacementSlot == -1) return;
            requestedSlot = event.slot;
        } else {
            requestedSlot = selectedPlacementSlot;
        }
        if (controlState == null && requestedSlot != selectedPlacementSlot) return;
        int currentSlot = controlState == null ? DeferredActionQueue.selectedSlot() : requestedSlot;
        if (currentSlot == selectedPlacementSlot) return;
        if (DeferredActionQueue.ensureSelected(this, selectedPlacementSlot, -1, true)) event.addClick();
    }

    @Subscribe
    private void onUseKey(UseKeyEvent event) {
        String[] controlState = SpearReachFeature.readSharedControlState();
        if (controlState == null) {
            if (!isSequenceActive()) return;
            if (pendingBlockHit == null) return;
            if (waitingForProjectile) return;
        }
        if (isOutsideWorld() || MinecraftClient.getInstance().currentScreen != null) return;
        if (DeferredActionQueue.selectedSlot() != selectedPlacementSlot) return;
        boolean requireSameFace = controlState == null ? placementStep == 0 : (placementStep & 1) != 0;
        boolean canUse = BlockRaycasts.matchesHit(pendingBlockHit, requireSameFace);
        if (controlState == null) {
            if (!canUse) return;
            canUse = rotations().isRotationReady();
        }
        if (!canUse) return;
        event.addClick();
        if (controlState == null) {
            rotations().acknowledgeRotation();
            if (placementStep != PLACE_CART) return;
        }
        interactionSubmitted = true;
    }

    @Subscribe
    private void onPacketSend(PacketSendEvent event) {
        String[] controlState = SpearReachFeature.readSharedControlState();
        Packet<?> packet = event.getPacket();
        if (controlState == null && !(packet instanceof PlayerActionC2SPacket)) return;
        PlayerActionC2SPacket action = (PlayerActionC2SPacket) packet;
        if (action.getAction() != PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) return;
        if (controlState == null && isOutsideWorld()) return;
        boolean active = isSequenceActive();
        if (controlState == null) {
            if (active) return;
            active = place.isSelected(placeOption2);
        }
        if (!active) return;
        ItemStack bow = client.player.getActiveItem();
        if (controlState == null && !(bow.getItem() instanceof BowItem)) return;
        boolean flame = ItemProperties.hasEnchantment(bow, Enchantments.FLAME);
        int useTicks;
        if (controlState == null) {
            if (!flame) return;
            useTicks = client.player.getItemUseTime();
        } else {
            useTicks = flame ? 1 : 0;
        }
        float pull = BowItem.getPullProgress(useTicks);
        if (pull < 0.1f) return;
        Rotation rotation = rotations().serverRotation();
        if (rotation == null) rotation = new Rotation(client.player.getYaw(), client.player.getPitch());
        Vec3d origin = client.player.getCameraPosVec(1.0f).subtract(0.0, 0.1, 0.0);
        Vec3d velocity = launchVelocity(rotation, pull);
        BlockImpactProjectileSimulation simulation = new BlockImpactProjectileSimulation(origin, velocity);
        int flightTicks = countFlightTicks(simulation);
        BlockHitResult bestHit = simulation.getBlockImpact();
        if (bestHit == null || bestHit.getSide() != Direction.UP) return;
        if (flightTicks > LatencyTracker.milliseconds() / 50 + LATENCY_MARGIN_TICKS + RELEASE_LOOKAHEAD_TICKS) return;
        List<Vec3d> velocities = new ArrayList<>();
        velocities.add(velocity);
        for (int adjustment : new int[]{-1, 1}) {
            float adjustedPull = BowItem.getPullProgress(useTicks + adjustment);
            if (adjustedPull >= 0.1f) velocities.add(launchVelocity(rotation, adjustedPull));
        }
        int bestScore = 0;
        int bestCollisionTick = -1;
        for (Vec3d candidateVelocity : velocities) {
            BlockHitResult hit = new BlockImpactProjectileSimulation(origin, candidateVelocity)
                    .findBlockHit(MAX_SIMULATION_TICKS);
            if (hit == null || hit.getSide() != Direction.UP) continue;
            BlockPos rail = hit.getBlockPos().up();
            int collisionTick = findCartCollisionTick(origin, velocity, 0, rail);
            if (collisionTick == -1) continue;
            int minimumLead = 2 + (client.world.getBlockState(rail).getBlock() instanceof AbstractRailBlock ? 0 : 1);
            int score = 0;
            for (Vec3d testVelocity : velocities) {
                if (findCartCollisionTick(origin, testVelocity, 0, rail) >= minimumLead) ++score;
            }
            if (score <= bestScore) continue;
            bestScore = score;
            bestCollisionTick = collisionTick;
            bestHit = hit;
        }
        if (bestCollisionTick == -1) return;
        if (startSequence(null, bestHit, bestCollisionTick)) projectileWaitUntilTick = client.player.age + flightTicks;
    }

    private void scanProjectiles() {
        String[] controlState = SpearReachFeature.readSharedControlState();
        boolean mayStart = isSequenceActive();
        if (controlState == null) {
            mayStart = !mayStart && (place.isSelected(placeOption) || place.isSelected(placeOption2)
                    || place.isSelected(placeOption3));
        }
        Iterator<Entity> entities = client.world.getEntities().iterator();
        if (controlState != null) {
            boolean continueScan = entities.hasNext();
            while (continueScan) continueScan = ((PersistentProjectileEntity) entities.next()).isOnFire();
            return;
        }
        while (entities.hasNext()) {
            Entity entity = entities.next();
            if (!(entity instanceof PersistentProjectileEntity projectile)) continue;
            if (projectile.getOwner() != client.player) continue;
            if (((PersistentProjectileEntityAccessor) projectile).invokeIsInGround()) {
                trackedProjectileIds.remove(projectile.getId());
                continue;
            }
            if (projectile.age < FRESH_ARROW_AGE) trackedProjectileIds.add(projectile.getId());
            if (trackedProjectile == null && projectileWaitUntilTick != -1
                    && lastProjectileVelocity == null && projectile.age == 0) {
                lastProjectilePosition = projectile.getEntityPos();
                lastProjectileVelocity = projectile.getVelocity();
            }
            if (!projectile.isOnFire()) continue;
            if (!mayStart) continue;
            if (projectile == lastSeenProjectile) continue;
            if (freshArrowsOnly.getValue() && !trackedProjectileIds.contains(projectile.getId())) continue;
            BlockHitResult hit = new BlockImpactProjectileSimulation(
                    projectile.getEntityPos(), projectile.getVelocity(), projectile).findBlockHit(MAX_SIMULATION_TICKS);
            if (hit == null || hit.getSide() != Direction.UP) continue;
            if (findCartCollisionTick(projectile.getEntityPos(), projectile.getVelocity(), projectile.age,
                    hit.getBlockPos().up()) == -1) continue;
            if (startSequence(projectile, hit, projectileFlightTicks(projectile))) {
                lastSeenProjectile = projectile;
                mayStart = false;
            }
        }
    }

    private boolean startSequence(PersistentProjectileEntity projectile, BlockHitResult hit, int flightTicks) {
        railPosition = hit.getBlockPos().up();
        targetHitPosition = hit.getPos();
        double range = client.player.getBlockInteractionRange();
        double rangeSquared = range * range;
        Vec3d eye = client.player.getCameraPosVec(1.0f);
        String[] controlState = SpearReachFeature.readSharedControlState();
        boolean railPresent = client.world.getBlockState(railPosition).getBlock() instanceof AbstractRailBlock;
        boolean placeRail = place.isSelected(placeOption);
        if (controlState != null) {
            if (placeRail) return railPresent;
            supportBlockPosition = null;
            return place.isSelected(placeOption3);
        }
        if (placeRail && !railPresent) {
            if (!PlacementWorldQueries.isReplaceable(railPosition)
                    || !Block.hasTopRim(client.world, railPosition.down())) return false;
            if (InventoryItems.findStackInHotbar(InventoryItems::isRail) == -1) return false;
            BlockHitResult railHit = BlockAimRotation.findHit(railPosition, targetHitPosition);
            if (railHit == null) return false;
            if (eye.squaredDistanceTo(railHit.getPos()) > rangeSquared) return false;
        }
        if (place.isSelected(placeOption2)) {
            if (!railPresent && !place.isSelected(placeOption)) return false;
            if (InventoryItems.findStackInHotbar(AutoCartFeature::isTntMinecart) == -1) return false;
            if (eye.squaredDistanceTo(BlockAimRotation.insetBottomPoint(railPosition, targetHitPosition))
                    > rangeSquared) return false;
        }
        supportBlockPosition = null;
        if (place.isSelected(placeOption3)) {
            if (findProtectionSlot() == -1) return false;
            Vec3d towardRail = Vec3d.ofCenter(railPosition).subtract(client.player.getEntityPos());
            BlockPos protection = client.player.getBlockPos().offset(Direction.getFacing(towardRail.x, 0.0, towardRail.z));
            if (!protection.equals(railPosition) && PlacementWorldQueries.isReplaceable(protection)
                    && !PlacementWorldQueries.intersectsPlayer(protection)
                    && !PlacementWorldQueries.hasBlockingEntity(protection)
                    && new Box(protection).raycast(eye,
                            BlockAimRotation.insetBottomPoint(railPosition, targetHitPosition)).isEmpty()) {
                supportBlockPosition = protection;
            }
        }
        if (projectile == null) supportBlockPosition = null;
        int extraSteps = (place.isSelected(placeOption) && !railPresent ? 1 : 0)
                + (supportBlockPosition != null ? 1 : 0);
        int leadTicks = projectile == null ? PREDICTED_SHOT_LEAD_TICKS
                : LatencyTracker.milliseconds() / 50 + LATENCY_MARGIN_TICKS;
        if (place.isSelected(placeOption2) && flightTicks < leadTicks + extraSteps) return false;
        trackedProjectile = projectile;
        return true;
    }

    private boolean updatePlacementStep() {
        String[] controlState = SpearReachFeature.readSharedControlState();
        placementStep = nextPlacementStep();
        if (controlState == null) {
            if (placementStep == -1) {
                finishSequence(true);
                return false;
            }
            pendingBlockHit = null;
        }
        int leadTicks = LatencyTracker.milliseconds() / 50 + LATENCY_MARGIN_TICKS;
        int remainingTicks = controlState != null || (placementStep == PLACE_CART && trackedProjectile != null)
                ? projectileFlightTicks(trackedProjectile) : 0;
        if (controlState == null) {
            if (placementStep == PLACE_CART && trackedProjectile != null && remainingTicks < leadTicks - 1) {
                finishSequence(false);
                return false;
            }
            if (placementStep == PLACE_CART && trackedProjectile == null && lastProjectileVelocity != null
                    && findCartCollisionTick(lastProjectilePosition, lastProjectileVelocity, 0, railPosition) < 3) {
                finishSequence(false);
                return false;
            }
            waitingForProjectile = placementStep == PLACE_CART && trackedProjectile != null && remainingTicks > leadTicks;
            selectedPlacementSlot = switch (placementStep) {
                case PLACE_RAIL -> InventoryItems.findStackInHotbar(InventoryItems::isRail);
                case PLACE_CART -> InventoryItems.findStackInHotbar(AutoCartFeature::isTntMinecart);
                default -> findProtectionSlot();
            };
            if (selectedPlacementSlot != -1) {
                if (placementStep == PLACE_CART) {
                    pendingBlockHit = new BlockHitResult(BlockAimRotation.insetBottomPoint(railPosition, targetHitPosition),
                            Direction.UP, railPosition, false);
                } else if (placementStep == PLACE_RAIL) {
                    pendingBlockHit = BlockAimRotation.findHit(railPosition, targetHitPosition);
                } else {
                    PlacementTarget target = PlacementTargets.find(supportBlockPosition, true);
                    if (target != null) pendingBlockHit = target.hitResult();
                }
            }
            if (pendingBlockHit != null) return true;
            if (placementStep == PLACE_PROTECTION) {
                supportBlockPosition = null;
                return updatePlacementStep();
            }
        } else {
            waitingForProjectile = trackedProjectile != null && (remainingTicks & 1) != 0;
            selectedPlacementSlot = placementStep;
        }
        finishSequence(false);
        return false;
    }

    private int nextPlacementStep() {
        String[] controlState = SpearReachFeature.readSharedControlState();
        if (controlState != null) return place.isSelected(placeOption2) ? 1 : 0;
        if (place.isSelected(placeOption3) && supportBlockPosition != null
                && PlacementWorldQueries.isReplaceable(supportBlockPosition)) return PLACE_PROTECTION;
        if (place.isSelected(placeOption)
                && !(client.world.getBlockState(railPosition).getBlock() instanceof AbstractRailBlock)) return PLACE_RAIL;
        if (place.isSelected(placeOption2) && !interactionSubmitted
                && client.world.getNonSpectatingEntities(TntMinecartEntity.class, new Box(railPosition)).isEmpty()) {
            return PLACE_CART;
        }
        return -1;
    }

    private static int projectileFlightTicks(PersistentProjectileEntity projectile) {
        return countFlightTicks(new BlockImpactProjectileSimulation(
                projectile.getEntityPos(), projectile.getVelocity(), projectile));
    }

    private static Vec3d launchVelocity(Rotation rotation, float pull) {
        Vec3d movement = client.player.getVelocity();
        Vec3d launch = Vec3d.fromPolar(rotation.pitch(), rotation.yaw()).multiply(pull * 3.0f);
        return launch.add(movement.x, client.player.isOnGround() ? 0.0 : movement.y, movement.z);
    }

    private static int findCartCollisionTick(Vec3d origin, Vec3d velocity, int elapsedTicks, BlockPos rail) {
        Box cart = new Box(rail.getX() + 0.01, rail.getY() + 0.0625, rail.getZ() + 0.01,
                rail.getX() + 0.99, rail.getY() + 0.7625, rail.getZ() + 0.99);
        String[] controlState = SpearReachFeature.readSharedControlState();
        BlockImpactProjectileSimulation simulation = new BlockImpactProjectileSimulation(origin, velocity);
        for (int tick = 1; tick <= MAX_SIMULATION_TICKS; ++tick) {
            boolean stopped = simulation.isStopped();
            if (controlState != null) return stopped ? 1 : 0;
            if (stopped) break;
            Vec3d previous = simulation.getPosition();
            simulation.step();
            double margin = Math.max(0.0, Math.min(0.3, (elapsedTicks + tick - 2) / 20.0));
            if (cart.expand(margin).raycast(previous, simulation.getPosition()).isPresent()) return tick;
        }
        return -1;
    }

    private static int countFlightTicks(BlockImpactProjectileSimulation simulation) {
        int ticks = 0;
        String[] controlState = SpearReachFeature.readSharedControlState();
        while (!simulation.isStopped()) {
            if (controlState != null) return ticks;
            if (ticks >= MAX_SIMULATION_TICKS) break;
            simulation.step();
            ++ticks;
        }
        return ticks;
    }

    private static int findProtectionSlot() {
        int slabSlot = InventoryItems.findStackInHotbar(AutoCartFeature::isSlabBlockItem);
        String[] controlState = SpearReachFeature.readSharedControlState();
        if (controlState != null || slabSlot != -1) return slabSlot;
        return InventoryItems.findStackInHotbar(AutoCartFeature::isProtectiveBlockItem);
    }

    private void finishSequence(boolean selectReturnItem) {
        String[] controlState = SpearReachFeature.readSharedControlState();
        if (controlState == null) resetSequence();
        int returnSlot = controlState != null || selectReturnItem ? resolveReturnSlot() : -1;
        if (controlState == null) {
            if (returnSlot == -1) {
                DeferredActionQueue.finish(this, true);
            } else {
                DeferredActionQueue.finish(this, false);
                InventoryItems.selectHotbarSlot(returnSlot);
            }
        } else {
            InventoryItems.selectHotbarSlot(returnSlot);
        }
        releaseRotation();
    }

    private int resolveReturnSlot() {
        String[] controlState = SpearReachFeature.readSharedControlState();
        boolean slotsSelected = returnTo.isSelected(returnToOption2);
        if (controlState != null) return slotsSelected ? 1 : 0;
        if (slotsSelected) return returnSlots.chooseSelectedSlot();
        if (returnTo.isSelected(returnToOption)) {
            return InventoryItems.findStackInHotbar(stack -> returnItems.isEntrySelected(stack.getItem()));
        }
        return InventoryItems.findStackInHotbar(AutoCartFeature::isBow);
    }

    private void resetSequence() {
        initializeProjectileType();
        trackedProjectile = null;
        initializePositionType();
        railPosition = null;
        initializePositionType();
        supportBlockPosition = null;
        initializeVectorType();
        targetHitPosition = null;
        pendingBlockHit = null;
        selectedPlacementSlot = -1;
        interactionSubmitted = false;
        projectileWaitUntilTick = -1;
        initializeVectorType();
        lastProjectilePosition = null;
        initializeVectorType();
        lastProjectileVelocity = null;
    }

    private boolean isSequenceActive() {
        String[] controlState = SpearReachFeature.readSharedControlState();
        if (controlState == null) {
            initializeProjectileType();
            if (trackedProjectile != null) return true;
            return projectileWaitUntilTick != -1;
        }
        return (projectileWaitUntilTick & 1) != 0;
    }

    private void releaseRotation() {
        String[] controlState = SpearReachFeature.readSharedControlState();
        boolean silent = silentRotations.getValue();
        if (controlState == null && silent) rotations().beginReturn(this);
        else rotations().cancel(this);
    }

    private void completeDisable() {
        String[] controlState = SpearReachFeature.readSharedControlState();
        if (controlState == null) {
            if (rotations().beginReturn(this)) return;
            disablePending = false;
        }
        super.onDisable();
    }

    private static boolean isTntMinecart(ItemStack stack) { return stack.isOf(Items.TNT_MINECART); }
    private static boolean isBow(ItemStack stack) { return stack.getItem() instanceof BowItem; }

    private static boolean isSlabBlockItem(ItemStack stack) {
        Item item = stack.getItem();
        String[] controlState = SpearReachFeature.readSharedControlState();
        if (controlState == null && !(item instanceof BlockItem)) return false;
        BlockItem blockItem = (BlockItem) item;
        return blockItem.getBlock() instanceof SlabBlock;
    }

    private static boolean isProtectiveBlockItem(ItemStack stack) {
        Item item = stack.getItem();
        String[] controlState = SpearReachFeature.readSharedControlState();
        if (controlState == null && !(item instanceof BlockItem)) return false;
        BlockItem blockItem = (BlockItem) item;
        boolean rail = blockItem.getBlock() instanceof AbstractRailBlock;
        return controlState != null ? rail : !rail;
    }

    private static void initializeProjectileType() {
        try {
            Class.forName(PersistentProjectileEntity.class.getName(), true, PersistentProjectileEntity.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }

    private static void initializePositionType() {
        try {
            Class.forName(BlockPos.class.getName(), true, BlockPos.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }

    private static void initializeVectorType() {
        try {
            Class.forName(Vec3d.class.getName(), true, Vec3d.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }

    public static void appendBootstrapSettings(AutoCartFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCartFeature.place);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCartFeature.silentRotations);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCartFeature.freshArrowsOnly);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCartFeature.returnTo);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCartFeature.returnSlots);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCartFeature.returnItems);
    }

    public static CharSequence bootstrapReturnToOption3() { return returnToOption3; }
    public static CharSequence bootstrapReturnToOption2() { return returnToOption2; }
    public static CharSequence bootstrapReturnToOption() { return returnToOption; }
    public static CharSequence bootstrapPlaceOption() { return placeOption; }
    public static CharSequence bootstrapPlaceOption2() { return placeOption2; }
    public static CharSequence bootstrapPlaceOption3() { return placeOption3; }

    public static void assignBootstrapSettings(AutoCartFeature feature, ItemListSetting preparedReturnItems,
            ChoiceSetting preparedReturnTo, BooleanSetting preparedSilentRotations, BooleanSetting preparedFreshArrowsOnly,
            HotbarSlotsSetting preparedReturnSlots, MultiChoiceSetting preparedPlace) {
        AutoCartFeature.returnItems = preparedReturnItems;
        AutoCartFeature.returnTo = preparedReturnTo;
        AutoCartFeature.silentRotations = preparedSilentRotations;
        AutoCartFeature.freshArrowsOnly = preparedFreshArrowsOnly;
        AutoCartFeature.returnSlots = preparedReturnSlots;
        AutoCartFeature.place = preparedPlace;
    }
}
