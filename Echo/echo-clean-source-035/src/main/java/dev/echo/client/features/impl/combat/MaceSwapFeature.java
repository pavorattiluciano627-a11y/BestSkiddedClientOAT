package dev.echo.client.features.impl.combat;

import static dev.echo.client.bootstrap.CatalogText.literal;

import com.mojang.blaze3d.platform.InputConstants;
import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.events.EventPriority;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.AttackKeyEvent;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.events.input.HotbarKeyEvent;
import dev.echo.client.events.interaction.EntityAttackEvent;
import dev.echo.client.events.interaction.ItemUsageTickEvent;
import dev.echo.client.events.network.PacketReceiveEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.features.internals.TargetsFeature;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.input.InputKey;
import dev.echo.client.input.KeybindingCache;
import dev.echo.client.mixin.KeyBindingAccessor;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.HotbarSlotsSetting;
import dev.echo.client.setting.ItemListSetting;
import dev.echo.client.setting.NamedRangeGroupSetting;
import dev.echo.client.setting.range.NamedRange;
import dev.echo.client.utils.interaction.EntityInteractions;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.items.ItemGroups;
import dev.echo.client.utils.targeting.AttackEvaluation;
import dev.echo.client.utils.targeting.EntityPredicates;
import dev.echo.client.utils.targeting.TargetSelection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.EntityHitResult;

/** Ordered mace selection, delayed shield-stun follow-up and spear double-tap attacks. */
public class MaceSwapFeature extends Feature {
    private static final CharSequence RETURN_SLOT = literal("Slot");
    private static final CharSequence RETURN_ITEM = literal("Item");
    private static final CharSequence RETURN_ORIGINAL = literal("Original");
    public static FloatRangeSetting delay;
    public static ItemListSetting itemWhitelist;
    public static HotbarSlotsSetting hotbarWhitelist;
    private static final float SMASH_THRESHOLD = 1.5f;
    private static final float BREACH_UPPER_BOUND = 7.0f;
    private static final float MAXIMUM_FALL_DISTANCE = 20.0f;
    private static final NamedRange breachRange = new NamedRange(literal("Breach"), SMASH_THRESHOLD, BREACH_UPPER_BOUND);
    private static final NamedRange windBurstRange = new NamedRange(literal("Wind Burst"), SMASH_THRESHOLD, MAXIMUM_FALL_DISTANCE);
    private static final NamedRange densityRange = new NamedRange(literal("Density"), SMASH_THRESHOLD, MAXIMUM_FALL_DISTANCE);
    private static final NamedRange fallbackRange = new NamedRange(literal("Fallback")).disabled();
    public static NamedRangeGroupSetting maceSwapRanges = new NamedRangeGroupSetting(literal("Priority Order"),
        breachRange, windBurstRange, densityRange, fallbackRange)
        .bounds(literal("Fall Distance"), 0.0f, MAXIMUM_FALL_DISTANCE, 0.1f, literal(" Blocks"))
        .band(literal("Any"), 0.0f, MAXIMUM_FALL_DISTANCE)
        .band(literal("Smash"), SMASH_THRESHOLD, MAXIMUM_FALL_DISTANCE)
        .band(literal("Ground"), 0.0f, SMASH_THRESHOLD);
    public static BooleanSetting stunSlam;
    public static FloatRangeSetting stunSwitchDelay;
    public static BooleanSetting spearDTap;
    public static FloatSetting minimumDamage;
    public static ChoiceSetting returnTo;
    public static HotbarSlotsSetting returnSlots;
    public static ItemListSetting returnItems;
    private static boolean restoreUseKey = false;
    private static int scheduledMaceSlot = -1;
    private static int scheduledTargetId = -1;
    private static long scheduledSwapTick = -1L;
    private int pendingMaceSlot;
    private int pendingTargetId;
    private boolean requireSameTarget;
    private boolean attackPending;
    private long lastSwapTick;
    private static String delaySection;
    private static String itemConditionsSection;
    private static String hotbarConditionsSection;
    private static String prioritySection;
    private static String stunSection;
    private static String stunDelaySection;
    private static String spearSection;
    private static String damageSection;
    private static String returnChoiceSection;
    private static String returnSelectionSection;

    public MaceSwapFeature() {
        this(new FeatureInfo(literal("Mace Swap"), literal("Swaps to mace for attacks"), Category.COMBAT));
    }

    public MaceSwapFeature(FeatureInfo info) { super(info); }

    @Override
    public void postInitialize() {
        resetPendingAttack();
        stunSwitchDelay.setVisibilityPredicate(ignored -> stunSlam.getValue());
        stunSwitchDelay.description(literal("Total ticks from the shield disable to attack with the mace"));
        minimumDamage.setVisibilityPredicate(ignored -> spearDTap.getValue());
        returnSlots.setVisibilityPredicate(ignored -> returnTo.isSelected(RETURN_SLOT));
        returnItems.setVisibilityPredicate(ignored -> returnTo.isSelected(RETURN_ITEM));
        if (delaySection == null) delaySection = new String("Timing");
        delay.tooltip(delaySection);
        if (itemConditionsSection == null) itemConditionsSection = new String("Conditions");
        itemWhitelist.tooltip(itemConditionsSection);
        if (hotbarConditionsSection == null) hotbarConditionsSection = new String("Conditions");
        hotbarWhitelist.tooltip(hotbarConditionsSection);
        if (prioritySection == null) prioritySection = new String("Mace Priority");
        maceSwapRanges.tooltip(prioritySection);
        maceSwapRanges.description(literal("Top to bottom, the first entry with a usable mace inside its fall distance wins. Disabled entries block maces carrying them"));
        if (stunSection == null) stunSection = new String("Stun Slam");
        stunSlam.tooltip(stunSection);
        if (stunDelaySection == null) stunDelaySection = new String("Stun Slam");
        stunSwitchDelay.tooltip(stunDelaySection);
        if (spearSection == null) spearSection = new String("Spear D-tap");
        spearDTap.tooltip(spearSection);
        if (damageSection == null) damageSection = new String("Spear D-tap");
        minimumDamage.tooltip(damageSection);
        if (returnChoiceSection == null) returnChoiceSection = new String("Return");
        returnTo.tooltip(returnChoiceSection);
        if (returnSelectionSection == null) returnSelectionSection = new String("Return");
        returnSlots.tooltip(returnSelectionSection);
        returnItems.tooltip(returnSelectionSection);
    }

    @Override
    public void onEnable() {
        DeferredActionQueue.finish(this, true);
        resetSharedState();
        resetPendingAttack();
        initializeLivingEntityClass();
        TargetsFeature.currentTarget = null;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        DeferredActionQueue.finish(this, true);
        resetSharedState();
        resetPendingAttack();
        initializeLivingEntityClass();
        TargetsFeature.currentTarget = null;
        super.onDisable();
    }

    private static void initializeLivingEntityClass() {
        try {
            Class.forName(LivingEntity.class.getName(), true, LivingEntity.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (pendingMaceSlot == -1 || event.slot != pendingMaceSlot) return;
        if (client.player == null || (long) client.player.tickCount - lastSwapTick > 2L) {
            resetPendingAttack();
            return;
        }
        pendingMaceSlot = -1;
        if (client.player.getInventory().getSelectedSlot() != event.slot) {
            int ticks = Math.max(0, (int) delay.sampleGaussian());
            if (!DeferredActionQueue.select(this, event.slot, ticks,
                    returnTo.isSelected(RETURN_ORIGINAL), this::onSwapEnded)) {
                resetPendingAttack();
                return;
            }
        }
        event.addClick();
    }

    @Subscribe(priority = EventPriority.LOW)
    private void onAttackKey(AttackKeyEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) return;
        if (attackPending) {
            boolean recent = (long) client.player.tickCount - lastSwapTick <= 2L;
            int targetId = pendingTargetId;
            boolean checkTarget = requireSameTarget;
            resetPendingAttack();
            boolean holdingMace = InventoryItems.mainHandStack().getItem() == Items.MACE;
            boolean validTarget = !checkTarget || isCrosshairTargetValid(targetId);
            if (recent && holdingMace && validTarget && !event.hasClicks()) event.addClick();
            return;
        }
        if (!event.hasClicks()) return;
        if (!(client.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)) return;
        int slot = findEligibleMace(target);
        if (slot == -1) return;
        suspendItemUse();
        DeferredActionQueue.select(this, slot, Math.max(0, (int) delay.sampleGaussian()),
            returnTo.isSelected(RETURN_ORIGINAL), this::onSwapEnded);
    }

    @Subscribe
    private void beforeInput(HandleInputEvent.Pre event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) return;
        if (client.options.keyAttack == null || ((KeyBindingAccessor) client.options.keyAttack).getTimesPressed() <= 0) return;
        if (client.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target) {
            requestMaceSwap(target);
        }
    }

    /** Original public setTask(IC,LivingEntity,I): all three scalar parameters were key pieces. */
    public boolean requestMaceSwap(LivingEntity target) {
        int slot = findEligibleMace(target);
        if (slot == -1) return false;
        suspendItemUse();
        pendingMaceSlot = slot;
        pendingTargetId = target.getId();
        requireSameTarget = true;
        attackPending = true;
        lastSwapTick = client.player.tickCount;
        return true;
    }

    private int findEligibleMace(LivingEntity target) {
        if (isOutsideWorld() || attackPending || pendingMaceSlot != -1) return -1;
        if (EntityInteractions.isUsingOffhand() || !itemWhitelist.acceptsHeldItem()
                || !hotbarWhitelist.permitsCurrentSlot()) return -1;
        if (DeferredActionQueue.hasPendingActions() && !DeferredActionQueue.contains(this)) return -1;
        if (!TargetSelection.isValidTarget(target)) return -1;
        if (target instanceof Player player && EntityPredicates.isBlocking(player)
                && !EntityPredicates.isOutsideBlockingArc(player)) return -1;
        int slot = findPriorityMace();
        if (slot == -1 || DeferredActionQueue.contains(this)
                || client.player.getInventory().getSelectedSlot() == slot) return -1;
        return slot;
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void processStunSwap(HandleInputEvent.Pre event) {
        if (isOutsideWorld()) return;
        if (MinecraftRenderState.getScreen() != null || !stunSlam.getValue()) {
            TargetsFeature.currentTarget = null;
            clearScheduledSwap();
            return;
        }
        TargetsFeature.currentTarget = null;
        if (scheduledMaceSlot == -1 || (long) client.player.tickCount < scheduledSwapTick) return;
        if (DeferredActionQueue.hasPendingActions() && !DeferredActionQueue.contains(this)
                && !DeferredActionQueue.contains(ShieldBreakerFeature.class)) {
            clearScheduledSwap();
            return;
        }
        int slot = scheduledMaceSlot;
        int targetId = scheduledTargetId;
        clearScheduledSwap();
        prepareStunAttack(slot, targetId);
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void afterShieldAttack(EntityAttackEvent.Post event) {
        if (isOutsideWorld() || !stunSlam.getValue() || event.getPlayer() != client.player) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        LivingEntity tracked = TargetsFeature.currentTarget;
        TargetsFeature.currentTarget = null;
        if (tracked == null || tracked.getId() != target.getId()) return;
        if (!InventoryItems.holdsAxe() || !TargetSelection.isValidTarget(target)) return;
        int slot = findPriorityMace();
        if (slot == -1) return;
        int ticks = Math.max(1, Math.round(stunSwitchDelay.sampleGaussian()));
        scheduledMaceSlot = slot;
        scheduledTargetId = target.getId();
        scheduledSwapTick = client.player.tickCount + ticks;
        TargetsFeature.shieldBreakActive = true;
    }

    private void prepareStunAttack(int slot, int targetId) {
        suspendItemUse();
        pendingMaceSlot = client.player.getInventory().getSelectedSlot() == slot ? -1 : slot;
        pendingTargetId = targetId;
        requireSameTarget = false;
        attackPending = true;
        lastSwapTick = client.player.tickCount;
        TargetsFeature.shieldBreakActive = true;
    }

    private int findPriorityMace() {
        float fallDistance = (float) client.player.fallDistance;
        for (NamedRange range : maceSwapRanges.getRanges()) {
            if (!range.isEnabled() || !maceSwapRanges.contains(range, fallDistance)) continue;
            int slot = findBestMace(enchantmentFor(range));
            if (slot != -1) return slot;
        }
        return -1;
    }

    private static int findBestMace(ResourceKey<Enchantment> enchantment) {
        int bestSlot = -1;
        int bestLevel = 0;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (!(stack.getItem() instanceof MaceItem) || hasDisabledEnchantment(stack)) continue;
            int level = enchantment == null ? (hasNoConfiguredEnchantment(stack) ? 1 : 0)
                : InventoryItems.getEnchantmentLevel(stack, enchantment);
            if (level > bestLevel) { bestSlot = slot; bestLevel = level; }
        }
        return bestSlot;
    }

    private static boolean hasDisabledEnchantment(ItemStack stack) {
        for (NamedRange range : maceSwapRanges.getRanges()) {
            ResourceKey<Enchantment> enchantment = enchantmentFor(range);
            if (enchantment != null && !range.isEnabled()
                    && InventoryItems.getEnchantmentLevel(stack, enchantment) > 0) return true;
        }
        return false;
    }

    private static boolean hasNoConfiguredEnchantment(ItemStack stack) {
        for (NamedRange range : maceSwapRanges.getRanges()) {
            ResourceKey<Enchantment> enchantment = enchantmentFor(range);
            if (enchantment != null && InventoryItems.getEnchantmentLevel(stack, enchantment) > 0) return false;
        }
        return true;
    }

    private static ResourceKey<Enchantment> enchantmentFor(NamedRange range) {
        if (range == densityRange) return Enchantments.DENSITY;
        if (range == breachRange) return Enchantments.BREACH;
        if (range == windBurstRange) return Enchantments.WIND_BURST;
        return null;
    }

    public int getStunDelayTicks(LivingEntity target) {
        if (!isEnabled() || !stunSlam.getValue() || !(target instanceof Player player)) return 0;
        if (!EntityPredicates.isBlocking(player) || EntityPredicates.isOutsideBlockingArc(player)) return 0;
        return Math.max(1, Math.round(stunSwitchDelay.getUpper()));
    }

    public static boolean allowsAxes() {
        if (itemWhitelist.getSelectedCount() == 0) return true;
        for (Item item : BuiltInRegistries.ITEM) {
            if (ItemGroups.axes().matches(item) && itemWhitelist.isEntrySelected(item)) return true;
        }
        return false;
    }

    public static boolean hasPendingSwap() { return DeferredActionQueue.contains(MaceSwapFeature.class); }

    @Subscribe
    private void onItemUsageTick(ItemUsageTickEvent event) {
        if (isOutsideWorld()) return;
        boolean selected = false;
        for (AttackEvaluation evaluation : event.getAttackEvaluations()) {
            if (!evaluation.isAvailable()) continue;
            float damage = evaluation.damage() ? evaluation.damageAmount() : 0.0f;
            // Original FCMPG accepts unordered (NaN) operands at this lower-bound check.
            if (!selected && spearDTap.getValue() && !(damage < minimumDamage.getValue())
                    && evaluation.target() instanceof LivingEntity target) selected = requestMaceSwap(target);
        }
    }

    @Subscribe
    private void onPacketReceived(PacketReceiveEvent event) {
        if (isOutsideWorld()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ClientboundRespawnPacket || packet instanceof ClientboundPlayerPositionPacket) {
            resetSharedState();
            resetPendingAttack();
            TargetsFeature.currentTarget = null;
        }
    }

    private static void resetSharedState() { restoreUseKey = false; clearScheduledSwap(); }
    private static void clearScheduledSwap() { scheduledMaceSlot = -1; scheduledTargetId = -1; scheduledSwapTick = -1L; }
    private void resetPendingAttack() {
        pendingMaceSlot = -1;
        pendingTargetId = -1;
        requireSameTarget = true;
        attackPending = false;
        lastSwapTick = -1L;
    }

    private boolean isCrosshairTargetValid(int targetId) {
        if (targetId < 0 || !(client.hitResult instanceof EntityHitResult hit)) return false;
        return hit.getEntity().getId() == targetId && hit.getEntity() instanceof LivingEntity target
            && target.isAlive() && !target.isRemoved() && TargetSelection.isValidTarget(target);
    }

    private void suspendItemUse() {
        if (client.player != null && (client.player.isUsingItem() || client.options.keyUse.isDown())) {
            client.options.keyUse.setDown(false);
            EntityInteractions.stopUsingItem();
        }
        if (!restoreUseKey && KeybindingCache.isPressed(client.options.keyUse)) restoreUseKey = true;
    }

    private void onSwapEnded() {
        if (!returnTo.isSelected(RETURN_ORIGINAL)) {
            int slot = returnTo.isSelected(RETURN_SLOT) ? returnSlots.chooseSelectedSlot()
                : InventoryItems.findStackInHotbar(stack -> returnItems.isEntrySelected(stack.getItem()));
            if (slot != -1) InventoryItems.selectHotbarSlot(slot);
        }
        restoreItemUse();
    }

    private void restoreItemUse() {
        if (!restoreUseKey) return;
        restoreUseKey = false;
        if (client.options.keyUse == null) return;
        InputConstants.Key key = ((KeyBindingAccessor) client.options.keyUse).getBoundKey();
        if (key == null) return;
        InputKey input = key.getType() == InputConstants.Type.MOUSE
            ? InputKey.fromMouseCode(key.getValue()) : InputKey.fromCode(key.getValue());
        if (input.isPressed()) client.options.keyUse.setDown(true);
    }

    public static void appendBootstrapSettings(MaceSwapFeature feature) {
        CatalogSettings.append(feature, delay);
        CatalogSettings.append(feature, itemWhitelist);
        CatalogSettings.append(feature, hotbarWhitelist);
        CatalogSettings.append(feature, maceSwapRanges);
        CatalogSettings.append(feature, stunSlam);
        CatalogSettings.append(feature, stunSwitchDelay);
        CatalogSettings.append(feature, spearDTap);
        CatalogSettings.append(feature, minimumDamage);
        CatalogSettings.append(feature, returnTo);
        CatalogSettings.append(feature, returnSlots);
        CatalogSettings.append(feature, returnItems);
    }

    public static void assignBootstrapSettings(MaceSwapFeature feature, BooleanSetting preparedSpearDTap,
            HotbarSlotsSetting preparedHotbarWhitelist, FloatRangeSetting preparedStunSwitchDelay,
            ItemListSetting preparedReturnItems, FloatSetting preparedMinimumDamage, ChoiceSetting preparedReturnTo,
            HotbarSlotsSetting preparedReturnSlots, ItemListSetting preparedItemWhitelist,
            BooleanSetting preparedStunSlam, FloatRangeSetting preparedDelay) {
        spearDTap = preparedSpearDTap;
        hotbarWhitelist = preparedHotbarWhitelist;
        stunSwitchDelay = preparedStunSwitchDelay;
        returnItems = preparedReturnItems;
        minimumDamage = preparedMinimumDamage;
        returnTo = preparedReturnTo;
        returnSlots = preparedReturnSlots;
        itemWhitelist = preparedItemWhitelist;
        stunSlam = preparedStunSlam;
        delay = preparedDelay;
    }
}
