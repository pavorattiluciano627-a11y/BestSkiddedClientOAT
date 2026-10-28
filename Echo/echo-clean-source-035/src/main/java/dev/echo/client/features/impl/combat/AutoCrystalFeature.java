package dev.echo.client.features.impl.combat;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.AttackKeyEvent;
import dev.echo.client.events.input.UseKeyEvent;
import dev.echo.client.events.interaction.BlockBreakingEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.HotbarSlotsSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.setting.ItemListSetting;
import dev.echo.client.setting.KeybindSetting;
import dev.echo.client.utils.ActionTimer;
import dev.echo.client.utils.items.InventoryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Crystal clicks and the staged obsidian/crystal hotbar-ownership sequence. */
public class AutoCrystalFeature extends Feature {
    private static final double NEARBY_DEATH_RADIUS = 6.0;
    private static final CharSequence returnToOption3 = CatalogText.literal("Original");
    private static final CharSequence returnToOption = CatalogText.literal("Slot");
    private static final CharSequence returnToOption2 = CatalogText.literal("Item");
    private static FloatRangeSetting placeDelay;
    private static FloatRangeSetting breakDelay;
    private static IntSetting failChance;
    private static BooleanSetting stopOnKill;
    private static BooleanSetting fakePunch;
    private static BooleanSetting placeObsidian;
    private static FloatRangeSetting switchDelay;
    private static FloatRangeSetting placeObsidianDelay;
    private static BooleanSetting switchBack;
    private static ChoiceSetting returnTo;
    private static HotbarSlotsSetting returnSlots;
    private static ItemListSetting returnItems;
    private static ItemListSetting itemWhitelist;
    private static HotbarSlotsSetting hotbarWhitelist;
    private static KeybindSetting activateKey;
    private ActionTimer placeTimer;
    private ActionTimer breakTimer;
    private CrystalPlacementStage stage;
    private long stageReadyAtTick;
    private BlockPos pendingObsidianPosition;
    private boolean obsidianPlacementPending;
    private boolean lastActionWasPlacement;
    private boolean actionSentThisTick;
    private static String placeDelayTooltip;
    private static String breakDelayTooltip;
    private static String placeObsidianTooltip;
    private static String obsidianOptionsTooltip;

    public AutoCrystalFeature() {
        super(new dev.echo.client.features.FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Auto Crystal"), dev.echo.client.bootstrap.CatalogText.literal("Automatically places and detonates crystals"), dev.echo.client.features.Category.COMBAT));
    }

    @Override
    public void postInitialize() {
        placeTimer = new ActionTimer();
        breakTimer = new ActionTimer();
        stage = CrystalPlacementStage.NONE;
        lastActionWasPlacement = true;
        itemWhitelist.setEntrySelected(Items.END_CRYSTAL, true);
        placeObsidian.description(CatalogText.literal("Hit Crystal, places obsidian and switches to crystal when you press the activate key"));
        fakePunch.description(CatalogText.literal("Spam attack and use clicks while holding a crystal, without checking for crystals"));
        itemWhitelist.description(CatalogText.literal("Items you can hold to start the Hit Crystal sequence"));
        switchDelay.setVisibilityPredicate(ignored -> placeObsidian.getValue());
        placeObsidianDelay.setVisibilityPredicate(ignored -> placeObsidian.getValue());
        switchBack.setVisibilityPredicate(ignored -> placeObsidian.getValue());
        returnTo.setVisibilityPredicate(ignored -> placeObsidian.getValue() && switchBack.getValue());
        returnSlots.setVisibilityPredicate(ignored -> placeObsidian.getValue() && switchBack.getValue() && returnTo.isSelected(returnToOption));
        returnItems.setVisibilityPredicate(ignored -> placeObsidian.getValue() && switchBack.getValue() && returnTo.isSelected(returnToOption2));
        itemWhitelist.setVisibilityPredicate(ignored -> placeObsidian.getValue());
        hotbarWhitelist.setVisibilityPredicate(ignored -> placeObsidian.getValue());
        if (placeDelayTooltip == null) placeDelayTooltip = new String("Timing");
        placeDelay.tooltip(placeDelayTooltip);
        if (breakDelayTooltip == null) breakDelayTooltip = new String("Timing");
        breakDelay.tooltip(breakDelayTooltip);
        if (placeObsidianTooltip == null) placeObsidianTooltip = new String("Hit Crystal");
        placeObsidian.tooltip(placeObsidianTooltip);
        if (obsidianOptionsTooltip == null) obsidianOptionsTooltip = new String("Hit Crystal");
        switchDelay.tooltip(obsidianOptionsTooltip);
        placeObsidianDelay.tooltip(obsidianOptionsTooltip);
        switchBack.tooltip(obsidianOptionsTooltip);
        returnTo.tooltip(obsidianOptionsTooltip);
        returnSlots.tooltip(obsidianOptionsTooltip);
        returnItems.tooltip(obsidianOptionsTooltip);
        itemWhitelist.tooltip(obsidianOptionsTooltip);
        hotbarWhitelist.tooltip(obsidianOptionsTooltip);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        placeTimer.reset();
        breakTimer.reset();
        lastActionWasPlacement = true;
        actionSentThisTick = false;
        resetSequence(false);
    }

    @Override
    public void onDisable() {
        resetSequence(false);
        lastActionWasPlacement = true;
        actionSentThisTick = false;
        super.onDisable();
    }

    @Subscribe
    private void onAttackKey(AttackKeyEvent event) {
        actionSentThisTick = false;
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null || activateKey.isListeningForKey()) return;
        if (!activateKey.getKey().isPressed()) {
            if (stage != CrystalPlacementStage.NONE) resetSequence(switchBack.getValue());
            lastActionWasPlacement = true;
            return;
        }
        if (stopOnKill.getValue() && hasNearbyDeadPlayer(NEARBY_DEATH_RADIUS)) {
            if (stage != CrystalPlacementStage.NONE) resetSequence(switchBack.getValue());
            return;
        }
        updateSequence();
        if (shouldFakePunch()) {
            if (!breakTimer.hasElapsedTicks(sampleDelayTicks(breakDelay))) return;
            if (!lastActionWasPlacement && placeTimer.hasElapsedTicks(sampleDelayTicks(placeDelay))) return;
            if (!rollFailure()) event.addClick();
            breakTimer.reset();
            lastActionWasPlacement = false;
            actionSentThisTick = true;
            return;
        }
        if (event.hasClicks() && shouldSuppressMining()) event.setClickCount(0);
        if (stage != CrystalPlacementStage.NONE && stage != CrystalPlacementStage.CRYSTAL) return;
        if (!InventoryItems.holdsInEitherHand(Items.END_CRYSTAL)) return;
        if (!(client.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof EndCrystal crystal)
                || crystal.isRemoved() || !crystal.isAlive()) return;
        if (!breakTimer.hasElapsedTicks(sampleDelayTicks(breakDelay))) return;
        if (!event.hasClicks() && !rollFailure()) event.addClick();
        breakTimer.reset();
    }

    @Subscribe
    private void onUseKey(UseKeyEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null || activateKey.isListeningForKey()) return;
        if (!activateKey.getKey().isPressed()) return;
        if (stopOnKill.getValue() && hasNearbyDeadPlayer(NEARBY_DEATH_RADIUS)) return;
        switch (stage) {
            case SWITCH_OBSIDIAN, SWITCH_CRYSTAL -> event.setClickCount(0);
            case PLACE_OBSIDIAN -> handleObsidianUse(event);
            case NONE, CRYSTAL -> handleCrystalUse(event);
        }
    }

    @Subscribe
    private void onBlockBreaking(BlockBreakingEvent.Pre event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null || activateKey.isListeningForKey()) return;
        if (!activateKey.getKey().isPressed() || !shouldSuppressMining()) return;
        if (client.gameMode.isDestroying()) client.gameMode.stopDestroyBlock();
        event.cancel();
    }

    private void updateSequence() {
        BlockState pointedState = getCrosshairBlockState();
        if (pointedState != null && pointedState.is(Blocks.RESPAWN_ANCHOR)) {
            if (stage != CrystalPlacementStage.NONE) resetSequence(switchBack.getValue());
            return;
        }
        if (stage == CrystalPlacementStage.NONE) {
            tryStartSequence();
            if (stage == CrystalPlacementStage.NONE) return;
        }
        CrystalPlacementStage previousStage = null;
        while (stage != previousStage) {
            previousStage = stage;
            switch (stage) {
                case SWITCH_OBSIDIAN -> {
                    if (client.player.tickCount < stageReadyAtTick) return;
                    int slot = findObsidianSlot();
                    if (slot == -1 || !DeferredActionQueue.ensureSelected(this, slot, -1, false)) {
                        resetSequence(false);
                        return;
                    }
                    enterStage(CrystalPlacementStage.PLACE_OBSIDIAN, sampleDelayTicks(placeObsidianDelay));
                }
                case SWITCH_CRYSTAL -> {
                    if (client.player.tickCount < stageReadyAtTick) return;
                    int slot = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.END_CRYSTAL);
                    if (slot == -1 || !DeferredActionQueue.ensureSelected(this, slot, -1, false)) {
                        resetSequence(false);
                        return;
                    }
                    stage = CrystalPlacementStage.CRYSTAL;
                    placeTimer.reset();
                }
                default -> { }
            }
        }
    }

    private void tryStartSequence() {
        if (!placeObsidian.getValue()) return;
        if (itemWhitelist.getSelectedCount() != 0
                && !itemWhitelist.isEntrySelected(client.player.getMainHandItem().getItem())) return;
        if (!hotbarWhitelist.permitsCurrentSlot()) return;
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos position = hit.getBlockPos();
        BlockState state = client.level.getBlockState(position);
        boolean hasCrystal = InventoryItems.containsItemInHotbar(item -> item == Items.END_CRYSTAL);
        if (isCrystalBase(state)) {
            if (hasCrystal) enterStage(CrystalPlacementStage.SWITCH_CRYSTAL, sampleDelayTicks(switchDelay));
            return;
        }
        if (state.isAir() || !hasCrystal || findObsidianSlot() == -1) return;
        BlockPos placement = state.canBeReplaced() ? position : position.relative(hit.getDirection());
        BlockState placementState = client.level.getBlockState(placement);
        if (!placementState.isAir() && !placementState.canBeReplaced()) return;
        enterStage(CrystalPlacementStage.SWITCH_OBSIDIAN, sampleDelayTicks(switchDelay));
    }

    private void handleObsidianUse(UseKeyEvent event) {
        if (event.getClickCount() > 1) event.setClickCount(1);
        if (client.player.tickCount < stageReadyAtTick) {
            event.setClickCount(0);
            return;
        }
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            event.setClickCount(0);
            return;
        }
        if (isCrystalBase(client.level.getBlockState(hit.getBlockPos()))) {
            enterStage(CrystalPlacementStage.SWITCH_CRYSTAL, sampleDelayTicks(switchDelay));
            event.setClickCount(0);
            return;
        }
        if (obsidianPlacementPending) {
            if (pendingObsidianPosition != null && isCrystalBase(client.level.getBlockState(pendingObsidianPosition))) {
                enterStage(CrystalPlacementStage.SWITCH_CRYSTAL, sampleDelayTicks(switchDelay));
                event.setClickCount(0);
                return;
            }
            obsidianPlacementPending = false;
            stageReadyAtTick = client.player.tickCount + Math.max(1, sampleDelayTicks(placeObsidianDelay));
            event.setClickCount(0);
            return;
        }
        Item held = client.player.getMainHandItem().getItem();
        if (held != Items.OBSIDIAN && held != Items.BEDROCK) {
            event.setClickCount(0);
            return;
        }
        BlockPos position = new BlockPlaceContext(client.player, InteractionHand.MAIN_HAND, client.player.getMainHandItem(), hit).getClickedPos();
        if (isCrystalBase(client.level.getBlockState(position))) {
            enterStage(CrystalPlacementStage.SWITCH_CRYSTAL, sampleDelayTicks(switchDelay));
            event.setClickCount(0);
            return;
        }
        pendingObsidianPosition = position;
        obsidianPlacementPending = true;
        if (!event.hasClicks()) event.addClick();
    }

    private void handleCrystalUse(UseKeyEvent event) {
        if (shouldFakePunch()) {
            if (actionSentThisTick) return;
            if (!placeTimer.hasElapsedTicks(sampleDelayTicks(placeDelay))) return;
            if (!rollFailure()) event.addClick();
            placeTimer.reset();
            lastActionWasPlacement = true;
            actionSentThisTick = true;
            return;
        }
        if (!InventoryItems.holdsInEitherHand(Items.END_CRYSTAL)) return;
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos position = hit.getBlockPos();
        if (!isCrystalBase(client.level.getBlockState(position)) || hasCrystalEntityCollision(position)) return;
        if (!placeTimer.hasElapsedTicks(sampleDelayTicks(placeDelay))) return;
        if (!event.hasClicks() && !rollFailure()) event.addClick();
        placeTimer.reset();
    }

    private void enterStage(CrystalPlacementStage next, int ticks) {
        stage = next;
        stageReadyAtTick = client.player.tickCount + ticks;
        pendingObsidianPosition = null;
        obsidianPlacementPending = false;
    }

    private void resetSequence(boolean restore) {
        int slot = restore ? resolveReturnSlot() : -1;
        if (slot == -1) {
            DeferredActionQueue.finish(this, restore);
        } else {
            DeferredActionQueue.finish(this, false);
            InventoryItems.selectHotbarSlot(slot);
        }
        stage = CrystalPlacementStage.NONE;
        stageReadyAtTick = 0L;
        pendingObsidianPosition = null;
        obsidianPlacementPending = false;
    }

    private int resolveReturnSlot() {
        if (returnTo.isSelected(returnToOption)) return returnSlots.chooseSelectedSlot();
        if (returnTo.isSelected(returnToOption2)) {
            return InventoryItems.findStackInHotbar(stack -> returnItems.isEntrySelected(stack.getItem()));
        }
        return -1;
    }

    private boolean shouldFakePunch() {
        return fakePunch.getValue() && InventoryItems.holdsInEitherHand(Items.END_CRYSTAL)
                && (stage == CrystalPlacementStage.NONE || stage == CrystalPlacementStage.CRYSTAL);
    }

    private boolean rollFailure() {
        if (failChance.getValue() <= 0) return false;
        return Math.random() * 100.0 < failChance.getValue();
    }

    private static int sampleDelayTicks(FloatRangeSetting setting) {
        return Math.max(0, Math.round(setting.sampleGaussian()));
    }

    private BlockState getCrosshairBlockState() {
        if (client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            return client.level.getBlockState(hit.getBlockPos());
        }
        return null;
    }

    private boolean shouldSuppressMining() {
        BlockState state = getCrosshairBlockState();
        return state != null && (stage != CrystalPlacementStage.NONE || isCrystalBase(state));
    }

    private int findObsidianSlot() {
        int obsidian = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.OBSIDIAN);
        return obsidian != -1 ? obsidian : InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.BEDROCK);
    }

    private static boolean isCrystalBase(BlockState state) {
        return state != null && (state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK));
    }

    private static boolean hasCrystalEntityCollision(BlockPos base) {
        if (client.level == null) return false;
        BlockPos above = base.above();
        if (!client.level.isEmptyBlock(above)) return false;
        double x = above.getX(), y = above.getY(), z = above.getZ();
        return !client.level.getEntities(null, new AABB(x, y, z, x + 1.0, y + 2.0, z + 1.0)).isEmpty();
    }

    private static boolean hasNearbyDeadPlayer(double range) {
        if (client.player == null || client.level == null) return false;
        double squaredRange = range * range;
        for (Player player : client.level.players()) {
            if (player != client.player && player.isDeadOrDying() && client.player.distanceToSqr(player) < squaredRange) return true;
        }
        return false;
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(AutoCrystalFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCrystalFeature.placeDelay);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCrystalFeature.breakDelay);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCrystalFeature.failChance);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCrystalFeature.stopOnKill);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCrystalFeature.fakePunch);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCrystalFeature.placeObsidian);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCrystalFeature.switchDelay);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCrystalFeature.placeObsidianDelay);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCrystalFeature.switchBack);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCrystalFeature.returnTo);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCrystalFeature.returnSlots);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCrystalFeature.returnItems);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCrystalFeature.itemWhitelist);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCrystalFeature.hotbarWhitelist);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoCrystalFeature.activateKey);
    }

    public static java.lang.CharSequence bootstrapReturnToOption3() { return returnToOption3; }
    public static java.lang.CharSequence bootstrapReturnToOption() { return returnToOption; }
    public static java.lang.CharSequence bootstrapReturnToOption2() { return returnToOption2; }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(AutoCrystalFeature feature, dev.echo.client.setting.FloatRangeSetting preparedPlaceDelay, dev.echo.client.setting.FloatRangeSetting preparedPlaceObsidianDelay, dev.echo.client.setting.BooleanSetting preparedSwitchBack, dev.echo.client.setting.BooleanSetting preparedFakePunch, dev.echo.client.setting.FloatRangeSetting preparedBreakDelay, dev.echo.client.setting.FloatRangeSetting preparedSwitchDelay, dev.echo.client.setting.BooleanSetting preparedPlaceObsidian, dev.echo.client.setting.HotbarSlotsSetting preparedHotbarWhitelist, dev.echo.client.setting.ItemListSetting preparedItemWhitelist, dev.echo.client.setting.HotbarSlotsSetting preparedReturnSlots, dev.echo.client.setting.IntSetting preparedFailChance, dev.echo.client.setting.ChoiceSetting preparedReturnTo, dev.echo.client.setting.KeybindSetting preparedActivateKey, dev.echo.client.setting.ItemListSetting preparedReturnItems, dev.echo.client.setting.BooleanSetting preparedStopOnKill) {
        AutoCrystalFeature.placeDelay = preparedPlaceDelay;
        AutoCrystalFeature.placeObsidianDelay = preparedPlaceObsidianDelay;
        AutoCrystalFeature.switchBack = preparedSwitchBack;
        AutoCrystalFeature.fakePunch = preparedFakePunch;
        AutoCrystalFeature.breakDelay = preparedBreakDelay;
        AutoCrystalFeature.switchDelay = preparedSwitchDelay;
        AutoCrystalFeature.placeObsidian = preparedPlaceObsidian;
        AutoCrystalFeature.hotbarWhitelist = preparedHotbarWhitelist;
        AutoCrystalFeature.itemWhitelist = preparedItemWhitelist;
        AutoCrystalFeature.returnSlots = preparedReturnSlots;
        AutoCrystalFeature.failChance = preparedFailChance;
        AutoCrystalFeature.returnTo = preparedReturnTo;
        AutoCrystalFeature.activateKey = preparedActivateKey;
        AutoCrystalFeature.returnItems = preparedReturnItems;
        AutoCrystalFeature.stopOnKill = preparedStopOnKill;
    }
}
