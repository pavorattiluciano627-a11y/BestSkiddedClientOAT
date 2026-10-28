package dev.echo.client.features.impl.combat;

import static dev.echo.client.bootstrap.CatalogText.literal;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.events.EventPriority;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.AttackKeyEvent;
import dev.echo.client.events.input.HotbarKeyEvent;
import dev.echo.client.events.input.UseKeyEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.features.internals.TargetsFeature;
import dev.echo.client.handlers.AttackCooldownTracker;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.input.KeybindingCache;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.HotbarSlotsSetting;
import dev.echo.client.setting.ItemListSetting;
import dev.echo.client.utils.ActionTimer;
import dev.echo.client.utils.interaction.EntityInteractions;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.targeting.EntityPredicates;
import dev.echo.client.utils.targeting.TargetSelection;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

/** Shield-breaking attack input, axe selection, timing, stun follow-up and own-shield restoration. */
public class ShieldBreakerFeature extends Feature {
    private static final CharSequence modeOption = literal("Click");
    private static final CharSequence modeOption2 = literal("Normal");
    private static final CharSequence returnToOption = literal("Original");
    private static final CharSequence returnToOption2 = literal("Slot");
    private static final CharSequence returnToOption3 = literal("Item");
    public static ChoiceSetting mode;
    public static FloatRangeSetting reactionDelay;
    public static FloatRangeSetting attackDelay;
    public static BooleanSetting autoSwap;
    public static BooleanSetting swapBack;
    public static FloatRangeSetting swapBackDelay;
    public static ChoiceSetting returnTo;
    public static HotbarSlotsSetting returnSlots;
    public static ItemListSetting returnItems;
    public static BooleanSetting unblockOwnShield;
    public static BooleanSetting stun;
    public static ItemListSetting itemWhitelist;
    public static HotbarSlotsSetting hotbarWhitelist;
    private ActionTimer attackTimer;
    private ActionTimer switchTimer;
    private int selectedAxeSlot;
    private Player shieldTarget;
    private Player previousShieldTarget;
    private Player timedAttackTarget;
    private boolean attackDelayActive;
    private boolean switchDelayActive;
    private boolean ownShieldReleased;
    private boolean restoreShieldRequested;
    private boolean unblockTickPending;
    private static String reactionSection;
    private static String attackSection;
    private static String returnChoiceSection;
    private static String returnSelectionSection;
    private static String itemConditionsSection;
    private static String hotbarConditionsSection;

    public ShieldBreakerFeature() {
        this(new FeatureInfo(literal("Shield Breaker"), literal("Breaks enemy shields"), Category.COMBAT));
    }

    public ShieldBreakerFeature(FeatureInfo info) { super(info); }

    @Override
    public void postInitialize() {
        attackTimer = new ActionTimer();
        switchTimer = new ActionTimer();
        selectedAxeSlot = -1;
        reactionDelay.setVisibilityPredicate(ignored -> mode.isSelected(modeOption2));
        attackDelay.setVisibilityPredicate(ignored -> mode.isSelected(modeOption2));
        autoSwap.setVisibilityPredicate(ignored -> mode.isSelected(modeOption2));
        unblockOwnShield.setVisibilityPredicate(ignored -> mode.isSelected(modeOption2));
        swapBack.setVisibilityPredicate(ignored -> mode.isSelected(modeOption)
            || mode.isSelected(modeOption2) && autoSwap.getValue());
        swapBackDelay.setVisibilityPredicate(ignored -> swapBack.getValue()
            && (mode.isSelected(modeOption) || mode.isSelected(modeOption2) && autoSwap.getValue()));
        returnTo.setVisibilityPredicate(ignored -> usesReturnSelection());
        returnSlots.setVisibilityPredicate(ignored -> usesReturnSelection() && returnTo.isSelected(returnToOption2));
        returnItems.setVisibilityPredicate(ignored -> usesReturnSelection() && returnTo.isSelected(returnToOption3));
        if (reactionSection == null) reactionSection = new String("Timing");
        reactionDelay.tooltip(reactionSection);
        if (attackSection == null) attackSection = new String("Timing");
        attackDelay.tooltip(attackSection);
        swapBackDelay.tooltip(attackSection);
        if (returnChoiceSection == null) returnChoiceSection = new String("Return");
        returnTo.tooltip(returnChoiceSection);
        if (returnSelectionSection == null) returnSelectionSection = new String("Return");
        returnSlots.tooltip(returnSelectionSection);
        returnItems.tooltip(returnSelectionSection);
        if (itemConditionsSection == null) itemConditionsSection = new String("Conditions");
        itemWhitelist.tooltip(itemConditionsSection);
        if (hotbarConditionsSection == null) hotbarConditionsSection = new String("Conditions");
        hotbarWhitelist.tooltip(hotbarConditionsSection);
    }

    private static boolean usesReturnSelection() {
        return swapBack.getValue() && (mode.isSelected(modeOption)
            || mode.isSelected(modeOption2) && autoSwap.getValue());
    }

    @Override
    public void onEnable() {
        resetShieldBreakState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        DeferredActionQueue.finish(this, true);
        resetShieldBreakState();
        super.onDisable();
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (event.slot == 0) {
            releaseOwnShield();
            prepareAxeSwap();
        }
        if (unblockTickPending) {
            event.setClickCount(0);
            return;
        }
        if (event.slot != selectedAxeSlot) return;
        selectedAxeSlot = -1;
        int ticks = Math.max(0, Math.round(swapBackDelay.sampleGaussian()));
        boolean restoreAll = swapBack.getValue() && returnTo.isSelected(returnToOption);
        if (!DeferredActionQueue.select(this, event.slot, ticks, restoreAll, this::onSwapEnded)) {
            shieldTarget = null;
            return;
        }
        event.addClick();
    }

    @Subscribe(priority = EventPriority.LOWEST)
    private void onAttackKey(AttackKeyEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) return;
        if (unblockTickPending) {
            unblockTickPending = false;
            TargetsFeature.shieldBreakActive = false;
            event.setClickCount(0);
            return;
        }
        if (previousShieldTarget != null) {
            previousShieldTarget = null;
            if (!TargetsFeature.shieldBreakActive && !event.hasClicks()) event.addClick();
            TargetsFeature.shieldBreakActive = false;
            return;
        }
        TargetsFeature.shieldBreakActive = false;
        if (shieldTarget != null) {
            Player target = shieldTarget;
            shieldTarget = null;
            if (InventoryItems.holdsAxe()) addShieldAttack(event, target);
            return;
        }
        if (mode.isSelected(modeOption)) handleClickAttack(event);
        else handleNormalAttack(event);
    }

    @Subscribe
    private void onUseKey(UseKeyEvent event) {
        if (!ownShieldReleased) return;
        if (restoreShieldRequested) {
            restoreShieldRequested = false;
            ownShieldReleased = false;
            if (KeybindingCache.isPressed(client.options.keyUse) && EntityInteractions.holdsShield()) {
                client.options.keyUse.setDown(true);
                if (!event.hasClicks()) event.addClick();
            }
            return;
        }
        if (KeybindingCache.isPressed(client.options.keyUse)) {
            client.options.keyUse.setDown(false);
            event.setClickCount(0);
        } else {
            ownShieldReleased = false;
        }
    }

    private void handleClickAttack(AttackKeyEvent event) {
        if (!event.hasClicks() || !passesItemConditions()) return;
        if (DeferredActionQueue.hasPendingActions() && !DeferredActionQueue.contains(this)) return;
        if (EntityInteractions.isUsingOffhand()) return;
        Player target = findShieldTarget();
        if (target == null || client.missTime > 0 || AttackCooldownTracker.hasNonFireHurtWindow(target)) return;
        if (InventoryItems.holdsAxe()) {
            addShieldAttack(event, target);
            return;
        }
        int slot = findAxeSlot();
        if (slot == -1 || DeferredActionQueue.contains(this)) return;
        int ticks = Math.max(0, Math.round(swapBackDelay.sampleGaussian()));
        boolean restoreAll = swapBack.getValue() && returnTo.isSelected(returnToOption);
        if (!DeferredActionQueue.select(this, slot, ticks, restoreAll, this::onSwapEnded)) return;
        addShieldAttack(event, target);
    }

    private void handleNormalAttack(AttackKeyEvent event) {
        if (!passesItemConditions()) return;
        if (DeferredActionQueue.hasPendingActions() && !DeferredActionQueue.contains(this)) return;
        Player target = findShieldTarget();
        if (target == null) { resetShieldAttackTiming(); return; }
        if (EntityInteractions.isBlocking() || client.missTime > 0
                || AttackCooldownTracker.hasNonFireHurtWindow(target)) return;
        if (!InventoryItems.holdsAxe() || attackDelayActive) return;
        if (!switchTimer.hasElapsedMillis((double) attackDelay.sampleGaussian())) return;
        addShieldAttack(event, target);
    }

    private void prepareAxeSwap() {
        if (!mode.isSelected(modeOption2) || isOutsideWorld()) return;
        if (MinecraftRenderState.getScreen() != null || !passesItemConditions()) return;
        if (DeferredActionQueue.hasPendingActions() && !DeferredActionQueue.contains(this)) return;
        Player target = findShieldTarget();
        if (target == null) { resetShieldAttackTiming(); return; }
        if (target != timedAttackTarget) {
            timedAttackTarget = target;
            attackTimer.reset();
            switchTimer.reset();
        }
        if (unblockTickPending || EntityInteractions.isBlocking() || InventoryItems.holdsAxe()) return;
        if (client.missTime > 0 || AttackCooldownTracker.hasNonFireHurtWindow(target)) return;
        if (!autoSwap.getValue() || switchDelayActive) return;
        if (!attackTimer.hasElapsedMillis((double) reactionDelay.sampleGaussian())) return;
        int slot = findAxeSlot();
        if (slot == -1 || DeferredActionQueue.contains(this)) return;
        selectedAxeSlot = slot;
        shieldTarget = target;
        switchDelayActive = true;
        switchTimer.reset();
    }

    private void addShieldAttack(AttackKeyEvent event, Player target) {
        if (!event.hasClicks()) event.addClick();
        TargetsFeature.currentTarget = target;
        if (stun.getValue()) previousShieldTarget = target;
        attackDelayActive = true;
        switchTimer.reset();
    }

    private Player findShieldTarget() {
        if (!(client.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof Player target)) return null;
        if (!EntityPredicates.isBlocking(target) || EntityPredicates.isOutsideBlockingArc(target)) return null;
        if (!TargetSelection.isValidTarget(target) || TargetSelection.resolveTarget(target) != target) return null;
        return target;
    }

    private boolean passesItemConditions() {
        return ownShieldReleased || DeferredActionQueue.contains(this)
            || itemWhitelist.acceptsHeldItem() && hotbarWhitelist.permitsCurrentSlot();
    }

    private int findAxeSlot() {
        return InventoryItems.findStackInHotbar(InventoryItems::isAxe);
    }

    private void releaseOwnShield() {
        if (!mode.isSelected(modeOption2) || !unblockOwnShield.getValue() || ownShieldReleased) return;
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null || !passesItemConditions()) return;
        if (DeferredActionQueue.hasPendingActions() && !DeferredActionQueue.contains(this)) return;
        Player target = findShieldTarget();
        if (target == null || !EntityInteractions.isBlocking() || AttackCooldownTracker.hasNonFireHurtWindow(target)) return;
        client.options.keyUse.setDown(false);
        EntityInteractions.stopUsingItem();
        ownShieldReleased = true;
        unblockTickPending = true;
    }

    private void onSwapEnded() {
        if (swapBack.getValue() && !returnTo.isSelected(returnToOption)) {
            int slot = returnTo.isSelected(returnToOption2) ? returnSlots.chooseSelectedSlot()
                : InventoryItems.findStackInHotbar(stack -> returnItems.isEntrySelected(stack.getItem()));
            if (slot != -1) InventoryItems.selectHotbarSlot(slot);
        }
        if (!mode.isSelected(modeOption2) || !ownShieldReleased) return;
        if (swapBack.getValue()) restoreShieldRequested = true;
        else ownShieldReleased = false;
    }

    private void resetShieldAttackTiming() {
        initializePlayerEntityClass();
        timedAttackTarget = null;
        attackDelayActive = false;
        switchDelayActive = false;
        initializeActionTimerClass();
        ActionTimer firstTimer = attackTimer;
        initializeActionTimerClass();
        firstTimer.reset();
        initializeActionTimerClass();
        ActionTimer secondTimer = switchTimer;
        initializeActionTimerClass();
        secondTimer.reset();
    }

    private void resetShieldBreakState() {
        selectedAxeSlot = -1;
        initializePlayerEntityClass();
        shieldTarget = null;
        initializePlayerEntityClass();
        previousShieldTarget = null;
        ownShieldReleased = false;
        restoreShieldRequested = false;
        unblockTickPending = false;
        TargetsFeature.shieldBreakActive = false;
        resetShieldAttackTiming();
    }

    private static void initializePlayerEntityClass() {
        try {
            Class.forName(Player.class.getName(), true, Player.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }

    private static void initializeActionTimerClass() {
        try {
            Class.forName(ActionTimer.class.getName(), true, ActionTimer.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }

    public static void appendBootstrapSettings(ShieldBreakerFeature feature) {
        CatalogSettings.append(feature, mode);
        CatalogSettings.append(feature, reactionDelay);
        CatalogSettings.append(feature, attackDelay);
        CatalogSettings.append(feature, autoSwap);
        CatalogSettings.append(feature, swapBack);
        CatalogSettings.append(feature, swapBackDelay);
        CatalogSettings.append(feature, returnTo);
        CatalogSettings.append(feature, returnSlots);
        CatalogSettings.append(feature, returnItems);
        CatalogSettings.append(feature, unblockOwnShield);
        CatalogSettings.append(feature, stun);
        CatalogSettings.append(feature, itemWhitelist);
        CatalogSettings.append(feature, hotbarWhitelist);
    }

    public static CharSequence bootstrapReturnToOption3() { return returnToOption3; }
    public static CharSequence bootstrapReturnToOption2() { return returnToOption2; }
    public static CharSequence bootstrapReturnToOption() { return returnToOption; }
    public static CharSequence bootstrapModeOption2() { return modeOption2; }
    public static CharSequence bootstrapModeOption() { return modeOption; }

    public static void assignBootstrapSettings(ShieldBreakerFeature feature, BooleanSetting preparedUnblockOwnShield,
            HotbarSlotsSetting preparedHotbarWhitelist, HotbarSlotsSetting preparedReturnSlots,
            BooleanSetting preparedSwapBack, ChoiceSetting preparedReturnTo, ChoiceSetting preparedMode,
            FloatRangeSetting preparedAttackDelay, BooleanSetting preparedAutoSwap, FloatRangeSetting preparedReactionDelay,
            ItemListSetting preparedReturnItems, BooleanSetting preparedStun, FloatRangeSetting preparedSwapBackDelay,
            ItemListSetting preparedItemWhitelist) {
        unblockOwnShield = preparedUnblockOwnShield;
        hotbarWhitelist = preparedHotbarWhitelist;
        returnSlots = preparedReturnSlots;
        swapBack = preparedSwapBack;
        returnTo = preparedReturnTo;
        mode = preparedMode;
        attackDelay = preparedAttackDelay;
        autoSwap = preparedAutoSwap;
        reactionDelay = preparedReactionDelay;
        returnItems = preparedReturnItems;
        stun = preparedStun;
        swapBackDelay = preparedSwapBackDelay;
        itemWhitelist = preparedItemWhitelist;
    }
}
