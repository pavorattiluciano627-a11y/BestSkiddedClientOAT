package dev.echo.client.features.impl.combat;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.events.input.MouseMovementEvent;
import dev.echo.client.events.input.SelectedSlotEvent;
import dev.echo.client.events.interaction.ItemUseEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.network.PacketReceiveEvent;
import dev.echo.client.events.network.PacketSendEvent;
import dev.echo.client.events.render.WorldRenderEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.handlers.AimTransition;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.handlers.RotationPriority;
import dev.echo.client.handlers.RotationManager;
import dev.echo.client.handlers.RotationSettings;
import dev.echo.client.handlers.RotationVisibility;
import dev.echo.client.input.InputKey;
import dev.echo.client.input.KeybindingCache;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.render.WorldProjection;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.setting.KeybindSetting;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.placement.AnchorController;
import dev.echo.client.utils.placement.BlockPlacement;
import dev.echo.client.utils.placement.PlacementTarget;
import dev.echo.client.utils.targeting.BlockFaceSamples;
import dev.echo.client.utils.targeting.BlockRaycasts;
import dev.echo.client.utils.targeting.ExplosionDamage;
import dev.echo.client.utils.targeting.TargetSelection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

/** Ordered placement, safety-block, charging and detonation state machine. */
public class AutoAnchorFeature extends Feature {
    private static final CharSequence styleOption = CatalogText.literal("Echo");
    private static final CharSequence styleOption2 = CatalogText.literal("Classic");
    private static final CharSequence modeOption = CatalogText.literal("Hold");
    private static final CharSequence modeOption2 = CatalogText.literal("Click");
    private static final int USE_COOLDOWN_TICKS = 4;
    private static final int RETURN_DELAY_TICKS = 1;
    private static final int MAX_CHARGES = 4;
    private static final float ORIGINAL_RANGE_CONSTANT = 1.7f;
    private static final int EXPLODED_ANCHOR_TIMEOUT_TICKS = 20;
    private static final int PHASE_TIMEOUT_TICKS = 40;
    private static ChoiceSetting style;
    private static AnchorController anchorController = new AnchorController(ignored -> !isEchoStyle());
    private static ChoiceSetting mode;
    private static KeybindSetting activateKey;
    private static BooleanSetting glowstoneStart;
    private static BooleanSetting repeat;
    private static BooleanSetting autoAim;
    private static BooleanSetting silentAim;
    private static FloatSetting rotationSpeed;
    private static BooleanSetting renderPosition;
    private static ColorSetting fillColor;
    private static ColorSetting outlineColor;
    private static BooleanSetting canPlaceLegit;
    private static BooleanSetting safeAnchor;
    private static IntSetting safetyTimeout;
    private static FloatSetting minDamage;
    private static FloatRangeSetting safeBlockDelay;
    private static FloatRangeSetting charges;
    private static FloatRangeSetting chargeDelay;
    private static IntSetting explodeSlot;
    private static FloatRangeSetting explodeDelay;
    private boolean sequenceRequested;
    private boolean activateWasPressed;
    private boolean restorePending;
    private boolean holdSequenceCompleted;
    private int useCooldownTicks;
    private Phase phase;
    private BlockPos anchorPosition;
    private BlockPos queuedAnchor;
    private int chargesRemaining;
    private int nextActionTick;
    private boolean glowstoneStartActive;
    private Vec3d anchorAimPoint;
    private long safetyStartMillis;
    private boolean interacting;
    private boolean suppressPacketFlush;
    private Vec3d lastSentPosition;
    private BlockPos recentlyExplodedAnchor;
    private int explodedAtTick;
    private int lastUseAttemptTick;
    private int lastUseCompletedTick;
    private static String renderSection, colorSection, chargesSection, actionTimingSection;
    private static String classicChargeToggleSection, classicChargeSection, classicExplodeToggleSection, classicExplodeSection;
    private static String classicSafetyToggleSection, classicSafetySection, itemConditionsSection, hotbarConditionsSection;
    private static String switchDelaySection, switchChanceSection;

    private enum Phase { PLACE_SAFETY, CHARGE, EXPLODE }

    public AutoAnchorFeature() {
        super(new FeatureInfo(CatalogText.literal("Auto Anchor"),
            CatalogText.literal("Automates anchor placement, charging, and detonation near your target"), Category.COMBAT));
    }

    private static boolean isEchoStyle() { return style.isSelected(styleOption); }

    @Override
    public void postInitialize() {
        lastUseAttemptTick = -1;
        lastUseCompletedTick = -1;
        activateKey.description(CatalogText.literal("If this matches your Use Key, start the sequence while holding a respawn anchor so vanilla use packets are not duplicated"));
        glowstoneStart.description(CatalogText.literal("Hold the activate key and right-click glowstone on a placed anchor to charge and detonate it"));
        autoAim.description(CatalogText.literal("Rotate for anchor placement, safety blocks, and charge or explode. Same-use-key placement stays manual"));
        canPlaceLegit.description(CatalogText.literal("Require the crosshair raycast to hit the correct block face for placements and anchor interactions"));
        safeAnchor.description(CatalogText.literal("Place a block between you and the anchor before charging, preferring the spot that reduces self-damage most"));
        safetyTimeout.description(CatalogText.literal("After this long, skip safety if placement still is not possible and you can interact with the anchor. 0 disables the timeout"));
        minDamage.description(CatalogText.literal("Skip the safety block if the anchor wouldn't deal at least this much damage to you"));
        safeBlockDelay.description(CatalogText.literal("Random delay before placing the safety block"));
        // Each original predicate captured a discarded key and was freshly allocated.
        mode.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle(); } });
        activateKey.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle(); } });
        glowstoneStart.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle(); } });
        repeat.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle() && mode.isSelected(modeOption); } });
        autoAim.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle(); } });
        silentAim.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle() && autoAim.getValue(); } });
        rotationSpeed.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle() && autoAim.getValue(); } });
        renderPosition.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle(); } });
        fillColor.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle() && renderPosition.getValue(); } });
        outlineColor.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle() && renderPosition.getValue(); } });
        canPlaceLegit.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle(); } });
        safeAnchor.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle(); } });
        safetyTimeout.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle() && safeAnchor.getValue(); } });
        minDamage.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle() && safeAnchor.getValue(); } });
        safeBlockDelay.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle() && safeAnchor.getValue(); } });
        charges.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle(); } });
        chargeDelay.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle(); } });
        explodeSlot.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle(); } });
        explodeDelay.setVisibilityPredicate(new Predicate<>() { public boolean test(Object ignored) { return isEchoStyle(); } });
        settings.addAll(anchorController.getSettings());
        if (renderSection == null) renderSection = new String("Appearance");
        renderPosition.tooltip(renderSection);
        if (colorSection == null) colorSection = new String("Appearance");
        fillColor.tooltip(colorSection);
        outlineColor.tooltip(colorSection);
        if (chargesSection == null) chargesSection = new String("Timing");
        charges.tooltip(chargesSection);
        if (actionTimingSection == null) actionTimingSection = new String("Timing");
        chargeDelay.tooltip(actionTimingSection);
        explodeSlot.tooltip(actionTimingSection);
        explodeDelay.tooltip(actionTimingSection);
        if (classicChargeToggleSection == null) classicChargeToggleSection = new String("Charge");
        anchorController.autoCharge.tooltip(classicChargeToggleSection);
        if (classicChargeSection == null) classicChargeSection = new String("Charge");
        anchorController.chargeChance.tooltip(classicChargeSection);
        anchorController.charges.tooltip(classicChargeSection);
        anchorController.glowstoneDelay.tooltip(classicChargeSection);
        anchorController.glowstoneChance.tooltip(classicChargeSection);
        if (classicExplodeToggleSection == null) classicExplodeToggleSection = new String("Explode");
        anchorController.autoExplode.tooltip(classicExplodeToggleSection);
        if (classicExplodeSection == null) classicExplodeSection = new String("Explode");
        anchorController.explodeSlot.tooltip(classicExplodeSection);
        anchorController.explodeDelay.tooltip(classicExplodeSection);
        anchorController.explodeChance.tooltip(classicExplodeSection);
        if (classicSafetyToggleSection == null) classicSafetyToggleSection = new String("Safe Anchor");
        anchorController.safeAnchor.tooltip(classicSafetyToggleSection);
        if (classicSafetySection == null) classicSafetySection = new String("Safe Anchor");
        anchorController.safeBlockDelay.tooltip(classicSafetySection);
        if (itemConditionsSection == null) itemConditionsSection = new String("Conditions");
        anchorController.itemWhitelist.tooltip(itemConditionsSection);
        if (hotbarConditionsSection == null) hotbarConditionsSection = new String("Conditions");
        anchorController.hotbarWhitelist.tooltip(hotbarConditionsSection);
        if (switchDelaySection == null) switchDelaySection = new String("Switch");
        anchorController.switchDelay.tooltip(switchDelaySection);
        if (switchChanceSection == null) switchChanceSection = new String("Switch");
        anchorController.switchChance.tooltip(switchChanceSection);
    }

    @Override public CharSequence getSuffix() { return style.getValue(); }

    @Override
    public void onEnable() {
        super.onEnable();
        sequenceRequested = false;
        activateWasPressed = false;
        restorePending = false;
        holdSequenceCompleted = false;
        useCooldownTicks = 0;
        resetSequence();
        initializeAnchorControllerType();
        AnchorController controller = anchorController;
        initializeAnchorControllerType();
        controller.reset();
    }

    @Override
    public void onDisable() {
        // NB-085 null-guard branch; the non-null original branch is an open applicability gate.
        try {
            Class.forName(SpearReachFeature.class.getName(), true, SpearReachFeature.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        sequenceRequested = false;
        activateWasPressed = false;
        restorePending = false;
        holdSequenceCompleted = false;
        useCooldownTicks = 0;
        DeferredActionQueue.finish(this, !hasSequence());
        resetSequence();
        initializeRotationType();
        RotationManager rotations = rotations();
        initializeRotationType();
        if (rotations.isOwnedBy(this)) {
            initializeRotationType();
            RotationManager current = rotations();
            initializeRotationType();
            current.cancel(this);
        }
        super.onDisable();
    }

    @Subscribe
    private void onItemUse(ItemUseEvent.Pre event) {
        if (!isEchoStyle()) {
            anchorController.onItemUse(event);
            return;
        }
        if (event.getPlayer() != client.player || event.getHand() != Hand.MAIN_HAND) return;
        if (MinecraftRenderState.getScreen() != null || activateKey.isListeningForKey()) return;
        if (lastUseAttemptTick == client.player.age) {
            event.cancel();
            return;
        }
        if (handleGlowstoneStart(event)) return;
        if (event.getHit() instanceof BlockHitResult hit && isRecentlyExploded(hit.getBlockPos())) {
            event.cancel();
            return;
        }
        if (!activationUsesUseKey()) {
            if (wantsSequence() || DeferredActionQueue.contains(this)) event.cancel();
            return;
        }
        if (mode.isSelected(modeOption) && !activateKey.getKey().isPressed() && !hasSequence()) return;
        if (isManualChargedAnchorUse(event)) {
            sequenceRequested = false;
            resetSequence();
            finishWithoutRestore();
            useCooldownTicks = USE_COOLDOWN_TICKS;
            return;
        }
        boolean start = !hasSequence() && event.getStack().isOf(Items.RESPAWN_ANCHOR);
        if (start && anchorFromHit(event.getHit()) == null && findGlowstoneSlot() == -1) {
            sequenceRequested = false;
            return;
        }
        if (start) queuedAnchor = anchorFromHit(event.getHit());
        if (!(start || hasSequence() || DeferredActionQueue.contains(this) || restorePending || useCooldownTicks > 0)) return;
        if (!mode.isSelected(modeOption) && start) {
            sequenceRequested = true;
            if (!hasSequence()) {
                restorePending = false;
                useCooldownTicks = 0;
            }
        }
        event.cancel();
    }

    @Subscribe
    private void onItemUseComplete(ItemUseEvent.Post event) {
        if (!isEchoStyle()) {
            anchorController.onItemUseComplete(event);
            return;
        }
        if (client.player != null) lastUseCompletedTick = client.player.age;
    }

    /** The original subscriber is exactly a RETURN instruction. */
    @Subscribe private void onSelectedSlot(SelectedSlotEvent event) { }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        if (!isEchoStyle() || isOutsideWorld() || !renderPosition.getValue()) return;
        boolean fill = fillColor.getAlpha() > 0;
        boolean outline = outlineColor.getAlpha() > 0;
        if (!fill && !outline) return;
        PlacementTarget target = findAnchorPlacement(event.getTickProgress());
        if (target == null) return;
        BlockPos position = target.position();
        var geometry = event.getRenderContext().getDepthTestedGeometry();
        if (fill) geometry.drawBox(position.getX(), position.getY(), position.getZ(), 1.0, 1.0, 1.0, fillColor.getArgb());
        if (outline) WorldProjection.drawBoxOutline(geometry, new Box(position), outlineColor.getArgb());
    }

    @Subscribe
    private void onTick(ClientTickEvent event) {
        if (!isEchoStyle() || isOutsideWorld()) return;
        if (MinecraftRenderState.getScreen() == null && !activateKey.isListeningForKey()) return;
        sequenceRequested = false;
        cancelSequence();
        activateWasPressed = false;
    }

    @Subscribe
    private void onInputPre(HandleInputEvent.Pre event) {
        if (!isEchoStyle()) anchorController.onInputPre();
    }

    @Subscribe
    private void onInputPost(HandleInputEvent.Post event) {
        if (!isEchoStyle()) {
            anchorController.onInputPost();
            return;
        }
        if (useCooldownTicks > 0) useCooldownTicks--;
        if (!DeferredActionQueue.contains(this)) restorePending = false;
        if (recentlyExplodedAnchor != null) isRecentlyExploded(recentlyExplodedAnchor);
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null || activateKey.isListeningForKey()) {
            sequenceRequested = false;
            cancelSequence();
            activateWasPressed = false;
            return;
        }
        boolean sameUseKey = activationUsesUseKey();
        boolean pressed = activateKey.getKey().isPressed();
        boolean hold = mode.isSelected(modeOption);
        if (hold && !pressed) holdSequenceCompleted = false;
        if (!hold && pressed && !(activateWasPressed || sameUseKey && !holdingAnchor())) {
            sequenceRequested = true;
            if (!hasSequence()) {
                restorePending = false;
                useCooldownTicks = 0;
            }
        }
        boolean active = hold ? pressed : sequenceRequested || hasSequence();
        if (glowstoneStartActive) active = pressed;
        if (!active) {
            cancelSequence();
            activateWasPressed = pressed;
            return;
        }
        if (hold && !repeat.getValue() && holdSequenceCompleted && !hasSequence()) {
            cancelSequence();
            activateWasPressed = pressed;
            return;
        }
        if (sameUseKey && !hasSequence() && !holdingAnchor()) {
            if (!hold) sequenceRequested = false;
            if (!DeferredActionQueue.contains(this)) cancelSequence();
            activateWasPressed = pressed;
            return;
        }
        if (hasSequence()) {
            advanceSequence();
            activateWasPressed = pressed;
            return;
        }
        if (glowstoneStart.getValue() && useKeyPressed() && (!sameUseKey || client.player.getMainHandStack().isOf(Items.GLOWSTONE))) {
            activateWasPressed = pressed;
            return;
        }
        BlockPos anchor = consumeQueuedAnchor();
        if (anchor == null) {
            BlockHitResult hit = crosshairBlock();
            if (hit != null && isAnchor(hit.getBlockPos())) anchor = hit.getBlockPos();
        }
        if (anchor != null) {
            if (!canReach(anchor)) {
                if (!hold) sequenceRequested = false;
                cancelSequence();
                activateWasPressed = pressed;
                return;
            }
            beginSequence(anchor, requestedCharges());
            activateWasPressed = pressed;
            return;
        }
        if (glowstoneStart.getValue() && client.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
            activateWasPressed = pressed;
            return;
        }
        activateWasPressed = pressed;
        placeFreshAnchor(hold, sameUseKey);
    }

    private void placeFreshAnchor(boolean hold, boolean sameUseKey) {
        boolean useAim = autoAim.getValue() && !sameUseKey;
        PlacementTarget placement = null;
        BlockHitResult fallback = null;
        boolean aimed = false;
        if (useAim) {
            placement = findAnchorPlacement(tickDelta());
            if (placement != null) aimed = true;
            else fallback = crosshairBlock();
            if (fallback == null && !aimed) {
                if (!hold) sequenceRequested = false;
                cancelSequence();
                return;
            }
        } else {
            fallback = crosshairBlock();
            if (fallback == null) {
                if (!hold) sequenceRequested = false;
                cancelSequence();
                return;
            }
        }
        if (findGlowstoneSlot() == -1) {
            if (!hold) sequenceRequested = false;
            cancelSequence();
            return;
        }
        int slot = sameUseKey ? client.player.getInventory().getSelectedSlot()
            : InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.RESPAWN_ANCHOR));
        if (slot == -1) {
            sequenceRequested = false;
            cancelSequence();
            return;
        }
        restorePending = false;
        if (!sameUseKey && !DeferredActionQueue.ensureSelected(this, slot, -1, false)) return;
        if (useCooldownTicks > 0 || !client.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) return;
        BlockHitResult hit;
        BlockPos position;
        if (aimed) {
            if (placement == null || !canUsePlacement(placement)) return;
            hit = placement.hitResult();
            position = placement.position();
        } else {
            hit = fallback;
            position = BlockPlacement.placementPosition(hit);
            if (!isPlaceable(position)) {
                if (!hold) sequenceRequested = false;
                cancelSequence();
                return;
            }
            if (!BlockRaycasts.intersectsBlock(effectiveYaw(), effectivePitch(), hit.getBlockPos())) return;
            if (!rotations().isRotationReady() || !sentRayHits(hit.getBlockPos())) return;
        }
        if (isRecentlyExploded(hit.getBlockPos()) || usedItemThisTick()) return;
        ActionResult result = interact(hit);
        useCooldownTicks = USE_COOLDOWN_TICKS;
        if (result.isAccepted()) rotations().acknowledgeRotation();
        if (result.isAccepted() && position != null) beginSequence(position, requestedCharges());
        else if (!hold) finishSequence();
        if (hold && !result.isAccepted()) useCooldownTicks = Math.max(useCooldownTicks, 1);
    }

    @Subscribe
    private void onMouseMovement(MouseMovementEvent.Post event) {
        if (!isEchoStyle()) return;
        if (!autoAim.getValue() || isOutsideWorld() || MinecraftRenderState.getScreen() != null
                || !wantsSequence() || activationUsesUseKey() && !hasSequence()) {
            releaseAim();
            return;
        }
        Vec3d point = aimPoint();
        if (point == null) {
            releaseAim();
            return;
        }
        RotationSettings settings = RotationSettings.uniform(silentAim.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE,
            AimTransition.EASED, rotationSpeed());
        if (!rotations().aim(this, RotationPriority.HIGH, point, settings)) releaseAim();
    }

    private void beginSequence(BlockPos position, int desiredCharges) {
        anchorPosition = position;
        chargesRemaining = Math.max(0, desiredCharges - chargeCount(position));
        FloatRangeSetting delay;
        if (safeAnchor.getValue() && needsSafety(rankedSafetyCandidates())) {
            phase = Phase.PLACE_SAFETY;
            delay = safeBlockDelay;
        } else if (chargesRemaining <= 0) {
            phase = Phase.EXPLODE;
            delay = explodeDelay;
        } else {
            phase = Phase.CHARGE;
            delay = chargeDelay;
        }
        nextActionTick = client.player.age + sampleDelay(delay);
        restorePending = false;
        anchorAimPoint = BlockFaceSamples.findClearAimPoint(position, 1.0f);
        safetyStartMillis = System.currentTimeMillis();
    }

    private void advanceSequence() {
        if (!isAnchor(anchorPosition) || !canReach(anchorPosition)) {
            finishSequence();
            return;
        }
        if (client.player.age < nextActionTick) return;
        if (phase != Phase.PLACE_SAFETY && client.player.age - nextActionTick > PHASE_TIMEOUT_TICKS) {
            finishSequence();
            return;
        }
        switch (phase) {
            case PLACE_SAFETY -> placeSafety();
            case CHARGE -> chargeAnchor();
            case EXPLODE -> explodeAnchor();
        }
    }

    private void finishSequence() {
        resetSequence();
        if (mode.isSelected(modeOption) && activateKey.getKey().isPressed()) {
            if (!repeat.getValue()) {
                holdSequenceCompleted = true;
                finishWithoutRestore();
                releaseAim();
            } else if (activationUsesUseKey()) scheduleRestore(RETURN_DELAY_TICKS);
            return;
        }
        holdSequenceCompleted = false;
        sequenceRequested = false;
        finishWithoutRestore();
    }

    private void resetSequence() {
        initializePhaseType();
        phase = null;
        initializePositionType();
        anchorPosition = null;
        initializePositionType();
        queuedAnchor = null;
        chargesRemaining = 0;
        nextActionTick = 0;
        glowstoneStartActive = false;
        initializeVectorType();
        anchorAimPoint = null;
    }

    private boolean hasSequence() {
        initializePhaseType();
        if (phase == null) return false;
        initializePositionType();
        return anchorPosition != null;
    }

    private void enterChargePhase() {
        phase = Phase.CHARGE;
        nextActionTick = client.player.age + sampleDelay(chargeDelay);
    }

    private void enterExplodePhase() {
        if (chargeCount(anchorPosition) <= 0) {
            finishSequence();
            return;
        }
        phase = Phase.EXPLODE;
        nextActionTick = client.player.age + sampleDelay(explodeDelay);
    }

    private void placeSafety() {
        List<BlockPos> candidates = rankedSafetyCandidates();
        if (!needsSafety(candidates)) {
            enterChargePhase();
            return;
        }
        PlacementTarget placement = safetyPlacement(candidates);
        if (placement != null) useSafetyPlacement(placement);
        if (phase == Phase.PLACE_SAFETY && safetyTimedOut() && anchorHit(anchorPosition) != null) enterChargePhase();
    }

    private PlacementTarget safetyPlacement(List<BlockPos> candidates) {
        if (autoAim.getValue()) {
            PlacementTarget placement = firstSafetyPlacement(candidates);
            if (placement == null || !canUsePlacement(placement)) return null;
            return placement;
        }
        BlockHitResult hit = crosshairBlock();
        if (hit == null) return null;
        BlockPos support = hit.getBlockPos();
        if (support.equals(anchorPosition)) return null;
        if (!BlockRaycasts.intersectsBlock(effectiveYaw(), effectivePitch(), support)) return null;
        if (!rotations().isRotationReady() || !sentRayHits(support)) return null;
        BlockPos position = support.offset(hit.getSide());
        if (position.equals(anchorPosition) || !BlockPlacement.isReplaceable(position)
                || BlockPlacement.intersectsPlayer(position) || BlockPlacement.hasBlockingEntity(position)) return null;
        return new PlacementTarget(position, hit);
    }

    private boolean safetyTimedOut() {
        int timeout = safetyTimeout.getValue();
        return timeout > 0 && System.currentTimeMillis() - safetyStartMillis >= (long) timeout;
    }

    private void useSafetyPlacement(PlacementTarget placement) {
        int slot = findSafetyBlockSlot();
        if (slot == -1) {
            enterChargePhase();
            return;
        }
        if (!DeferredActionQueue.ensureSelected(this, slot, -1, false)) return;
        if (USE_COOLDOWN_TICKS - useCooldownTicks < 1) return;
        if (!(client.player.getMainHandStack().getItem() instanceof BlockItem) || usedItemThisTick()) return;
        ActionResult result = interact(placement.hitResult());
        useCooldownTicks = USE_COOLDOWN_TICKS;
        if (result.isAccepted()) {
            rotations().acknowledgeRotation();
            if (isSolidSafetyBlock(placement.position())) enterChargePhase();
        }
    }

    private boolean needsSafety(List<BlockPos> candidates) {
        if (!safeAnchor.getValue() || client.player == null || client.world == null || anchorPosition == null) return false;
        float minimum = minDamage.getValue();
        if (minimum > 0.0f && ExplosionDamage.anchorDamage(client.player, anchorPosition, null) < minimum) return false;
        BlockPos best = candidates.isEmpty() ? null : candidates.get(0);
        return best == null || !isSolidSafetyBlock(best);
    }

    private List<BlockPos> rankedSafetyCandidates() {
        if (client.player == null || anchorPosition == null) return List.of();
        BlockPos playerBlock = BlockPos.ofFloored(client.player.getLerpedPos(tickDelta()));
        Vec3d anchorCenter = Vec3d.ofCenter(anchorPosition);
        Vec3d playerCenter = client.player.getBoundingBox().getCenter();
        Vec3d towardAnchor = anchorCenter.subtract(playerCenter);
        LinkedHashSet<BlockPos> candidates = new LinkedHashSet<>(20);
        addSafetyRayCandidates(candidates, playerBlock, playerCenter, anchorCenter, towardAnchor);
        int bottom = client.player.isOnGround() ? 0 : -1;
        for (int y = 0; y >= bottom; y--) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    BlockPos position = playerBlock.add(x, y, z);
                    if (towardAnchor(position, playerCenter, towardAnchor)) candidates.add(position);
                }
            }
        }
        List<BlockPos> ranked = new ArrayList<>(candidates);
        ranked.sort(Comparator.comparingDouble(position -> ExplosionDamage.anchorDamage(client.player, anchorPosition, position)));
        return ranked;
    }

    private void addSafetyRayCandidates(Set<BlockPos> candidates, BlockPos playerBlock, Vec3d playerCenter,
            Vec3d anchorCenter, Vec3d towardAnchor) {
        Vec3d displacement = anchorCenter.subtract(playerCenter);
        double length = displacement.length();
        if (length < 1.0E-4) return;
        Vec3d direction = displacement.multiply(1.0 / length);
        int steps = Math.min(MathHelper.ceil(length), 3);
        for (int step = 1; step <= steps; step++) {
            BlockPos position = BlockPos.ofFloored(playerCenter.add(direction.multiply(step * 0.95)));
            if (position.equals(playerBlock) || position.equals(anchorPosition)
                    || Math.abs(position.getY() - playerBlock.getY()) > 1) continue;
            if (towardAnchor(position, playerCenter, towardAnchor)) candidates.add(position);
        }
    }

    private static boolean towardAnchor(BlockPos position, Vec3d playerCenter, Vec3d direction) {
        Vec3d displacement = Vec3d.ofCenter(position).subtract(playerCenter);
        return displacement.x * direction.x + displacement.z * direction.z > 0.0;
    }

    private PlacementTarget firstSafetyPlacement(List<BlockPos> candidates) {
        for (BlockPos position : candidates) {
            PlacementTarget placement = findSafetyPlacement(position);
            if (placement != null) return placement;
        }
        return null;
    }

    private PlacementTarget findSafetyPlacement(BlockPos position) {
        if (position == null || client.world == null || client.player == null || position.equals(anchorPosition)) return null;
        if (!BlockPlacement.isReplaceable(position) || !BlockPlacement.hasSupport(position)
                || BlockPlacement.intersectsPlayer(position) || BlockPlacement.hasBlockingEntity(position)) return null;
        PlacementTarget placement = BlockPlacement.find(position, canPlaceLegit.getValue());
        if (placement == null || placement.hitResult().getBlockPos().equals(anchorPosition)
                || isRecentlyExploded(placement.hitResult().getBlockPos())) return null;
        Vec3d eye = client.player.getCameraPosVec(1.0f);
        double reach = client.player.getBlockInteractionRange();
        if (eye.squaredDistanceTo(placement.hitResult().getPos()) > reach * reach) return null;
        return placement;
    }

    private boolean isSolidSafetyBlock(BlockPos position) {
        if (client.world == null || position == null) return false;
        BlockState state = client.world.getBlockState(position);
        return !state.isAir() && !state.isReplaceable();
    }

    private static int findGlowstoneSlot() {
        return InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.GLOWSTONE));
    }

    private int findSafetyBlockSlot() {
        int glowstone = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.GLOWSTONE));
        if (glowstone != -1) return glowstone;
        return InventoryItems.findStackInHotbar(new Predicate<>() {
            @Override public boolean test(net.minecraft.item.ItemStack stack) {
                return stack.getItem() instanceof BlockItem && !stack.isOf(Items.RESPAWN_ANCHOR);
            }
        });
    }

    private void chargeAnchor() {
        int existingCharges = chargeCount(anchorPosition);
        if (chargesRemaining <= 0 || existingCharges >= MAX_CHARGES) {
            enterExplodePhase();
            return;
        }
        int slot = findGlowstoneSlot();
        if (slot == -1) {
            chargesRemaining = 0;
            enterExplodePhase();
            return;
        }
        if (!DeferredActionQueue.ensureSelected(this, slot, -1, false)) return;
        if (!client.player.getMainHandStack().isOf(Items.GLOWSTONE)) return;
        BlockHitResult hit = anchorHit(anchorPosition);
        if (hit == null || usedItemThisTick()) return;
        ActionResult result = interact(hit);
        if (!result.isAccepted()) return;
        rotations().acknowledgeRotation();
        chargesRemaining--;
        if (chargesRemaining <= 0) {
            phase = Phase.EXPLODE;
            nextActionTick = client.player.age + sampleDelay(explodeDelay);
            return;
        }
        nextActionTick = client.player.age + sampleDelay(chargeDelay);
    }

    private void explodeAnchor() {
        int existingCharges = chargeCount(anchorPosition);
        if (existingCharges <= 0) {
            if (glowstoneStartActive) return;
            finishSequence();
            return;
        }
        int slot = explodeSlot.getValue() - 1;
        if (!DeferredActionQueue.ensureSelected(this, slot, -1, false)) return;
        if (client.player.getInventory().getStack(slot).isOf(Items.GLOWSTONE) && existingCharges < MAX_CHARGES) {
            finishSequence();
            return;
        }
        BlockHitResult hit = anchorHit(anchorPosition);
        if (hit == null || usedItemThisTick()) return;
        ActionResult result = interact(hit);
        if (!result.isAccepted()) return;
        rotations().acknowledgeRotation();
        useCooldownTicks = USE_COOLDOWN_TICKS;
        recentlyExplodedAnchor = anchorPosition;
        explodedAtTick = client.player.age;
        finishSequence();
    }

    private Vec3d aimPoint() {
        if (hasSequence()) {
            if (phase == Phase.PLACE_SAFETY) {
                List<BlockPos> candidates = rankedSafetyCandidates();
                if (needsSafety(candidates)) {
                    PlacementTarget placement = firstSafetyPlacement(candidates);
                    if (placement != null) return placement.hitResult().getPos();
                }
            }
            return anchorAimPoint(anchorPosition);
        }
        BlockHitResult hit = crosshairBlock();
        if (hit != null && isAnchor(hit.getBlockPos())) return hit.getPos();
        PlacementTarget placement = findAnchorPlacement(tickDelta());
        if (placement != null) return placement.hitResult().getPos();
        hit = crosshairBlock();
        return hit != null ? hit.getPos() : null;
    }

    private Vec3d anchorAimPoint(BlockPos position) {
        if (position == null) return null;
        if (client.player == null) return Vec3d.ofCenter(position);
        if (anchorAimPoint != null && position.equals(anchorPosition)) {
            Vec3d eye = client.player.getCameraPosVec(1.0f);
            if (BlockFaceSamples.rayHitsBlock(eye, anchorAimPoint, position)) return anchorAimPoint;
            anchorAimPoint = BlockFaceSamples.findClearAimPoint(position, 1.0f);
            return anchorAimPoint != null ? anchorAimPoint : Vec3d.ofCenter(position);
        }
        Vec3d point = BlockFaceSamples.findClearAimPoint(position, 1.0f);
        return point != null ? point : Vec3d.ofCenter(position);
    }

    private boolean canUsePlacement(PlacementTarget placement) {
        float yaw = effectiveYaw();
        float pitch = effectivePitch();
        if (!rotations().isRotationReady() || !sentRayHits(placement.hitResult().getBlockPos())) return false;
        if (!canPlaceLegit.getValue()) {
            BlockHitResult hit = placement.hitResult();
            if (!BlockPlacement.facesEye(hit, client.player.getCameraPosVec(1.0f))) return false;
            return BlockRaycasts.intersectsBlock(yaw, pitch, hit.getBlockPos());
        }
        BlockHitResult hit = BlockRaycasts.raycast(yaw, pitch);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return false;
        if (!hit.getBlockPos().equals(placement.hitResult().getBlockPos())) return false;
        if (hit.getSide() != placement.hitResult().getSide()) return false;
        return !rayHitsEntity(hit.getPos());
    }

    private BlockHitResult anchorHit(BlockPos position) {
        if (!canReach(position) || !rotations().isRotationReady() || !sentRayHits(position)) return null;
        float yaw = effectiveYaw();
        float pitch = effectivePitch();
        if (!canPlaceLegit.getValue()) {
            Vec3d eye = client.player.getCameraPosVec(1.0f);
            Vec3d end = eye.add(client.player.getRotationVector(pitch, yaw).multiply(client.player.getBlockInteractionRange() + 1.0));
            return VoxelShapes.fullCube().raycast(eye, end, position);
        }
        BlockHitResult hit = BlockRaycasts.raycast(yaw, pitch);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(position)) return null;
        return rayHitsEntity(hit.getPos()) ? null : hit;
    }

    private PlacementTarget findAnchorPlacement(float partialTick) {
        Vec3d eye = client.player.getCameraPosVec(partialTick);
        LivingEntity target = TargetSelection.findNearestTarget(eye, partialTick);
        if (target == null) return null;
        Vec3d currentEye = client.player.getCameraPosVec(1.0f);
        double reach = client.player.getBlockInteractionRange();
        double reachSquared = reach * reach;
        Vec3d targetPosition = target.getLerpedPos(partialTick);
        Vec3d displacement = targetPosition.subtract(target.getEntityPos());
        Box targetBox = target.getBoundingBox().offset(displacement);
        Vec3d feet = new Vec3d(targetPosition.x, targetBox.minY, targetPosition.z);
        int x = MathHelper.floor(feet.x), y = MathHelper.floor(feet.y), z = MathHelper.floor(feet.z);
        PlacementTarget best = null;
        double bestScore = Double.MAX_VALUE;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos position = new BlockPos(x + dx, y + dy, z + dz);
                    if (!BlockPlacement.isReplaceable(position) || !BlockPlacement.hasSupport(position)) continue;
                    PlacementTarget placement = BlockPlacement.find(position, canPlaceLegit.getValue());
                    if (placement == null || isRecentlyExploded(placement.hitResult().getBlockPos())) continue;
                    if (BlockPlacement.intersectsPlayer(position) || BlockPlacement.hasBlockingEntity(new Box(position))) continue;
                    if (currentEye.squaredDistanceTo(placement.hitResult().getPos()) > reachSquared) continue;
                    Vec3d center = Vec3d.ofCenter(position);
                    float exposure = ExplosionDamage.exposure(center, targetBox, target, null, null);
                    double distanceSquared = feet.squaredDistanceTo(center);
                    double score = (1.0 - (double) exposure) * 100.0 + distanceSquared * 0.01;
                    if (!(score < bestScore)) continue;
                    bestScore = score;
                    best = placement;
                }
            }
        }
        return best;
    }

    private boolean sentRayHits(BlockPos position) {
        if (lastSentPosition == null) return true;
        Box block = new Box(position);
        Vec3d eye = lastSentPosition.add(0.0, client.player.getStandingEyeHeight(), 0.0);
        if (block.contains(eye)) return true;
        Vec3d end = eye.add(client.player.getRotationVector(effectivePitch(), effectiveYaw()).multiply(client.player.getBlockInteractionRange()));
        return block.raycast(eye, end).isPresent();
    }

    private boolean usedItemThisTick() { return lastUseCompletedTick == client.player.age; }

    private ActionResult interact(BlockHitResult hit) {
        lastUseAttemptTick = client.player.age;
        interacting = true;
        try {
            return BlockPlacement.interactMainHand(hit, true);
        } finally {
            interacting = false;
        }
    }

    @Subscribe
    private void onPacketReceive(PacketReceiveEvent event) {
        if (!isEchoStyle() && event.getPacket() instanceof BlockUpdateS2CPacket update) anchorController.onBlockUpdate(update);
        if (!isEchoStyle() && event.getPacket() instanceof ExplosionS2CPacket explosion) anchorController.onExplosion(explosion.center());
    }

    @Subscribe
    private void onPacketSend(PacketSendEvent event) {
        if (event.getPacket() instanceof PlayerMoveC2SPacket movement && movement.changesPosition()) {
            lastSentPosition = new Vec3d(movement.getX(0.0), movement.getY(0.0), movement.getZ(0.0));
        }
        if (event.getPacket() instanceof PlayerMoveC2SPacket || event.getPacket() instanceof ClientTickEndC2SPacket) {
            suppressPacketFlush = false;
            return;
        }
        if (interacting || anchorController.isInteracting()) suppressPacketFlush = true;
        if (suppressPacketFlush) event.setFlush(false);
    }

    private boolean rayHitsEntity(Vec3d end) {
        Vec3d eye = client.player.getCameraPosVec(1.0f);
        return ProjectileUtil.raycast(client.player, eye, end, new Box(eye, end).expand(1.0),
            EntityPredicates.CAN_HIT, eye.squaredDistanceTo(end)) != null;
    }

    private float tickDelta() { return client.getRenderTickCounter().getTickProgress(false); }
    private float effectiveYaw() { return rotations().effectiveRotation().yaw(); }
    private float effectivePitch() { return rotations().effectiveRotation().pitch(); }

    private boolean wantsSequence() {
        if (mode.isSelected(modeOption)) {
            if (!activateKey.getKey().isPressed()) return false;
            return repeat.getValue() || !holdSequenceCompleted || hasSequence();
        }
        return sequenceRequested || hasSequence();
    }

    private boolean activationUsesUseKey() {
        if (client == null || client.options == null || client.options.useKey == null) return false;
        if (activateKey.getKey() == InputKey.NONE) return false;
        return activateKey.getKey() == KeybindingCache.getKey(client.options.useKey);
    }

    private boolean useKeyPressed() {
        return client.options != null && client.options.useKey != null && KeybindingCache.isPressed(client.options.useKey);
    }

    private boolean isManualChargedAnchorUse(ItemUseEvent.Pre event) {
        if (hasSequence() || !event.getStack().isOf(Items.RESPAWN_ANCHOR)) return false;
        BlockPos position = anchorFromHit(event.getHit());
        return position != null && chargeCount(position) > 0 && canReach(position);
    }

    private boolean handleGlowstoneStart(ItemUseEvent.Pre event) {
        if (!glowstoneStart.getValue() || !event.getStack().isOf(Items.GLOWSTONE) || !activateKey.getKey().isPressed()) return false;
        BlockPos position = anchorFromHit(event.getHit());
        if (position == null || !canReach(position)) return false;
        if (!hasSequence()) {
            anchorPosition = position;
            phase = Phase.EXPLODE;
            chargesRemaining = 0;
            glowstoneStartActive = true;
            restorePending = false;
            nextActionTick = client.player.age;
            anchorAimPoint = BlockFaceSamples.findClearAimPoint(position, 1.0f);
            advanceSequence();
        }
        if (chargeCount(position) > 0) event.cancel();
        return true;
    }

    private boolean holdingAnchor() { return client.player != null && client.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR); }

    private BlockHitResult crosshairBlock() {
        if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        return hit;
    }

    private BlockPos consumeQueuedAnchor() {
        BlockPos position = queuedAnchor;
        queuedAnchor = null;
        if (position == null) return null;
        return isAnchor(position) ? position : null;
    }

    private BlockPos anchorFromHit(HitResult hitResult) {
        if (!(hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        return isAnchor(hit.getBlockPos()) ? hit.getBlockPos() : null;
    }

    private boolean isPlaceable(BlockPos position) {
        return position != null && BlockPlacement.isReplaceable(position)
            && !BlockPlacement.intersectsPlayer(position) && !BlockPlacement.hasBlockingEntity(position);
    }

    private boolean isAnchor(BlockPos position) {
        return position != null && client.world != null && BlockPlacement.isBlock(position, Blocks.RESPAWN_ANCHOR)
            && !isRecentlyExploded(position);
    }

    private boolean isRecentlyExploded(BlockPos position) {
        if (recentlyExplodedAnchor == null || client.player == null || !recentlyExplodedAnchor.equals(position)) return false;
        if (client.player.age - explodedAtTick > EXPLODED_ANCHOR_TIMEOUT_TICKS || !BlockPlacement.isBlock(position, Blocks.RESPAWN_ANCHOR)) {
            recentlyExplodedAnchor = null;
            return false;
        }
        return true;
    }

    private int chargeCount(BlockPos position) {
        if (!isAnchor(position)) return 0;
        return client.world.getBlockState(position).get(RespawnAnchorBlock.CHARGES);
    }

    private boolean canReach(BlockPos position) {
        return client.player != null && position != null && client.player.canInteractWithBlockAt(position, 0.0);
    }

    private void cancelSequence() {
        boolean hadSequence = hasSequence();
        resetSequence();
        if (hadSequence) finishWithoutRestore();
        else if (DeferredActionQueue.contains(this) && !restorePending) scheduleRestore(RETURN_DELAY_TICKS);
        releaseAim();
    }

    private void scheduleRestore(int delay) {
        if (!DeferredActionQueue.contains(this)) return;
        int slot = DeferredActionQueue.selectedSlot(this);
        if (slot == -1) {
            DeferredActionQueue.finish(this, true);
            restorePending = false;
            return;
        }
        DeferredActionQueue.ensureSelected(this, slot, delay, true);
        restorePending = true;
    }

    private void finishWithoutRestore() {
        DeferredActionQueue.finish(this, false);
        restorePending = false;
    }

    private void releaseAim() {
        if (!rotations().isOwnedBy(this)) return;
        if (rotations().isSilentRotationActive() || silentAim.getValue()) rotations().beginReturn(this);
        else rotations().cancel(this);
    }

    private static float rotationSpeed() { return rotationSpeed.getValue() * 20.0f; }
    private int requestedCharges() { return MathHelper.clamp(Math.round(charges.sampleUniform()), 1, MAX_CHARGES); }
    private int sampleDelay(FloatRangeSetting delay) { return Math.max(0, Math.round(delay.sampleGaussian())); }

    private static void initializeAnchorControllerType() {
        try { Class.forName(AnchorController.class.getName(), true, AnchorController.class.getClassLoader()); }
        catch (ClassNotFoundException failure) { throw new IllegalStateException("Resolved class initialization failed", failure); }
    }

    private static void initializeRotationType() {
        try { Class.forName(RotationManager.class.getName(), true, RotationManager.class.getClassLoader()); }
        catch (ClassNotFoundException failure) { throw new IllegalStateException("Resolved class initialization failed", failure); }
    }

    private static void initializePhaseType() {
        try { Class.forName(Phase.class.getName(), true, Phase.class.getClassLoader()); }
        catch (ClassNotFoundException failure) { throw new IllegalStateException("Resolved class initialization failed", failure); }
    }

    private static void initializePositionType() {
        try { Class.forName(BlockPos.class.getName(), true, BlockPos.class.getClassLoader()); }
        catch (ClassNotFoundException failure) { throw new IllegalStateException("Resolved class initialization failed", failure); }
    }

    private static void initializeVectorType() {
        try { Class.forName(Vec3d.class.getName(), true, Vec3d.class.getClassLoader()); }
        catch (ClassNotFoundException failure) { throw new IllegalStateException("Resolved class initialization failed", failure); }
    }

    public static void appendBootstrapSettings(AutoAnchorFeature feature) {
        CatalogSettings.append(feature, style);
        CatalogSettings.append(feature, mode);
        CatalogSettings.append(feature, activateKey);
        CatalogSettings.append(feature, glowstoneStart);
        CatalogSettings.append(feature, repeat);
        CatalogSettings.append(feature, autoAim);
        CatalogSettings.append(feature, silentAim);
        CatalogSettings.append(feature, rotationSpeed);
        CatalogSettings.append(feature, renderPosition);
        CatalogSettings.append(feature, fillColor);
        CatalogSettings.append(feature, outlineColor);
        CatalogSettings.append(feature, canPlaceLegit);
        CatalogSettings.append(feature, safeAnchor);
        CatalogSettings.append(feature, safetyTimeout);
        CatalogSettings.append(feature, minDamage);
        CatalogSettings.append(feature, safeBlockDelay);
        CatalogSettings.append(feature, charges);
        CatalogSettings.append(feature, chargeDelay);
        CatalogSettings.append(feature, explodeSlot);
        CatalogSettings.append(feature, explodeDelay);
    }

    public static CharSequence bootstrapModeOption() { return modeOption; }
    public static CharSequence bootstrapModeOption2() { return modeOption2; }
    public static CharSequence bootstrapStyleOption() { return styleOption; }
    public static CharSequence bootstrapStyleOption2() { return styleOption2; }

    public static void assignBootstrapSettings(AutoAnchorFeature feature, FloatSetting preparedMinDamage,
            BooleanSetting preparedCanPlaceLegit, FloatRangeSetting preparedSafeBlockDelay, ColorSetting preparedOutlineColor,
            IntSetting preparedSafetyTimeout, ChoiceSetting preparedMode, BooleanSetting preparedGlowstoneStart,
            FloatRangeSetting preparedChargeDelay, BooleanSetting preparedRenderPosition, BooleanSetting preparedSafeAnchor,
            IntSetting preparedExplodeSlot, BooleanSetting preparedRepeat, ChoiceSetting preparedStyle, FloatRangeSetting preparedCharges,
            FloatRangeSetting preparedExplodeDelay, BooleanSetting preparedAutoAim, FloatSetting preparedRotationSpeed,
            ColorSetting preparedFillColor, KeybindSetting preparedActivateKey, BooleanSetting preparedSilentAim) {
        minDamage = preparedMinDamage;
        canPlaceLegit = preparedCanPlaceLegit;
        safeBlockDelay = preparedSafeBlockDelay;
        outlineColor = preparedOutlineColor;
        safetyTimeout = preparedSafetyTimeout;
        mode = preparedMode;
        glowstoneStart = preparedGlowstoneStart;
        chargeDelay = preparedChargeDelay;
        renderPosition = preparedRenderPosition;
        safeAnchor = preparedSafeAnchor;
        explodeSlot = preparedExplodeSlot;
        repeat = preparedRepeat;
        style = preparedStyle;
        charges = preparedCharges;
        explodeDelay = preparedExplodeDelay;
        autoAim = preparedAutoAim;
        rotationSpeed = preparedRotationSpeed;
        fillColor = preparedFillColor;
        activateKey = preparedActivateKey;
        silentAim = preparedSilentAim;
    }
}
