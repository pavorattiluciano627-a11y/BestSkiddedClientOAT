package dev.echo.client.features.impl.utility;

import dev.echo.client.Echo;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.config.FeatureNameAssignments;
import dev.echo.client.events.EventBus;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.lifecycle.WorldTickContextEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.utils.OwnerSet;
import dev.echo.client.utils.inventory.InventoryActionScheduler;
import dev.echo.client.utils.inventory.TemporaryItemUse;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Offhand health hysteresis and hotbar refill through the original two inventory state machines. */
public class AutoTotemFeature extends Feature {
    private static final CharSequence HEALTH_GROUP = CatalogText.literal("Health Swap");
    private static final CharSequence restoreItemOption = CatalogText.literal("Previous");
    private static final CharSequence restoreItemOption2 = CatalogText.literal("Shield");
    private static final int OFFHAND_SLOT = 40;
    private static final Item TOTEM = Items.TOTEM_OF_UNDYING;
    private static BooleanSetting refillHotbar;
    private static IntSetting slotToRefillTotem;
    private static BooleanSetting healthSwap;
    private static IntSetting equipBelow;
    private static IntSetting restoreAbove;
    private static ChoiceSetting restoreItem;
    private InventoryActionScheduler inventoryActions;
    private TemporaryItemUse temporaryItemUse;
    private Item replacedOffhandItem;
    private Item cachedDesiredItem;
    private long desiredItemTick;

    public AutoTotemFeature() {
        super(new dev.echo.client.features.FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Auto Totem"), dev.echo.client.bootstrap.CatalogText.literal("Automatically equips a totem in your offhand"), dev.echo.client.features.Category.UTILITY));
    }

    @Override
    public void postInitialize() {
        inventoryActions = new InventoryActionScheduler(CatalogText.literal("Mode"), this::getTransferItem,
                this::getDestinationSlot, this::canRunInventoryAction,
                // Exact original lambda$new$0: this scheduler option is always enabled.
                () -> true, true).withSlotPredicate(this::isSourceSlotAllowed).onTransfer(this::onInventoryTransfer);
        temporaryItemUse = new TemporaryItemUse(this, this::isSwapStillNeeded, this::onHandSwap);
        desiredItemTick = -1L;
        slotToRefillTotem.setVisibilityPredicate(ignored -> refillHotbar.getValue());
        equipBelow.setVisibilityPredicate(ignored -> healthSwap.getValue());
        restoreAbove.setVisibilityPredicate(ignored -> healthSwap.getValue());
        restoreItem.setVisibilityPredicate(ignored -> healthSwap.getValue());
        healthSwap.description(CatalogText.literal("Hold a totem only while your health is low and put your offhand item back once you heal"));
        equipBelow.description(CatalogText.literal("Swap a totem into your offhand at or below this health. Absorption is not counted"));
        restoreAbove.description(CatalogText.literal("Put the restore item back once your health is above this. Between the two values the offhand is left alone"));
        restoreItem.description(CatalogText.literal("Previous puts back whatever the totem replaced, Shield always puts a shield back"));
        if (!settings.contains(inventoryActions.getSettings().get(0))) {
            settings.addAll(0, inventoryActions.getSettings());
        }
        refillHotbar.tooltip("Appearance");
        slotToRefillTotem.tooltip("Appearance");
        healthSwap.tooltip(HEALTH_GROUP).restrict();
        equipBelow.tooltip(HEALTH_GROUP);
        restoreAbove.tooltip(HEALTH_GROUP);
        restoreItem.tooltip(HEALTH_GROUP);
    }

    @Override
    public void onEnable() {
        resetState();
        inventoryActions.reset();
        EventBus.register(inventoryActions);
        EventBus.register(temporaryItemUse);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        EventBus.unregister(inventoryActions);
        EventBus.unregister(temporaryItemUse);
        inventoryActions.reset();
        temporaryItemUse.cancelAndRestore();
        resetState();
        super.onDisable();
    }

    /** Original encodeToken(J)Z: reads controller state, independently of feature enablement. */
    public boolean shouldAllowTotemRemoval() {
        return temporaryItemUse.isSwapKeyActive();
    }

    @Subscribe
    private void onWorldContext(WorldTickContextEvent event) {
        resetState();
    }

    @Subscribe
    private void onTick(ClientTickEvent event) {
        if (isOutsideWorld() || client.gameMode == null) return;
        if (client.player.isDeadOrDying()) {
            resetState();
            return;
        }
        if (!healthSwap.getValue() || temporaryItemUse.isBusy()) return;
        if (OwnerSet.hasOwners() || inventoryActions.isActive()) return;
        Item desired = getDesiredOffhandItem();
        if (desired == null) return;
        int slot = findSwapSlot(desired);
        if (slot != -1) temporaryItemUse.beginSwap(slot, desired);
    }

    private boolean canRunInventoryAction() {
        return !temporaryItemUse.isBusy() && !OwnerSet.hasOwners();
    }

    private int getDestinationSlot() {
        if (temporaryItemUse.isBusy()) return -1;
        Item desired = getDesiredOffhandItem();
        if (desired != null) return shouldUseInventory(desired) ? OFFHAND_SLOT : -1;
        if (!refillHotbar.getValue()) return -1;
        int slot = slotToRefillTotem.getValue() - 1;
        return client.player.getInventory().getItem(slot).is(TOTEM) ? -1 : slot;
    }

    private boolean shouldUseInventory(Item item) {
        return inventoryActions.isActive() || findSwapSlot(item) == -1;
    }

    private Item getTransferItem() {
        if (isOutsideWorld()) return TOTEM;
        Item desired = getDesiredOffhandItem();
        return desired != null ? desired : TOTEM;
    }

    private boolean isSourceSlotAllowed(int slot) {
        if (!healthSwap.getValue() || getTransferItem() != TOTEM) return true;
        return slot != selectedSlot();
    }

    private Item getDesiredOffhandItem() {
        if (desiredItemTick == client.player.tickCount) return cachedDesiredItem;
        desiredItemTick = client.player.tickCount;
        cachedDesiredItem = chooseOffhandItem();
        return cachedDesiredItem;
    }

    private void onHandSwap(Item item) {
        desiredItemTick = -1L;
        announceTransfer(item);
    }

    private void onInventoryTransfer(int slot, Item item) {
        if (slot == OFFHAND_SLOT && healthSwap.getValue()) announceTransfer(item);
    }

    private void announceTransfer(Item item) {
        if (item == TOTEM) {
            FeatureNameAssignments.postNotification(this, CatalogText.literal("Swapped a totem into your offhand"));
            return;
        }
        String name = new ItemStack(item).getHoverName().getString();
        FeatureNameAssignments.postNotification(this, new StringBuilder("Put your ").append(name).append(" back"));
    }

    private Item chooseOffhandItem() {
        ItemStack offhand = client.player.getOffhandItem();
        if (!healthSwap.getValue()) return offhand.is(TOTEM) ? null : TOTEM;
        if (shouldEquipForHealth()) return chooseEmergencyTotem(offhand);
        if (shouldRestoreForHealth() && replacedOffhandItem != null && offhand.is(replacedOffhandItem)) {
            replacedOffhandItem = null;
        }
        Item restore = chooseRestoreItem(offhand);
        if (restore != null) return restore;
        return shouldFillOffhand(offhand) && hasUnselectedTotem() ? TOTEM : null;
    }

    private Item chooseEmergencyTotem(ItemStack offhand) {
        if (offhand.is(TOTEM) || !hasUnselectedTotem()) return null;
        if (!offhand.isEmpty()) replacedOffhandItem = offhand.getItem();
        return TOTEM;
    }

    private Item chooseRestoreItem(ItemStack offhand) {
        if (!shouldRestoreForHealth()) return null;
        if (!offhand.isEmpty() && !offhand.is(TOTEM)) return null;
        Item item = restoreItem.isSelected(restoreItemOption2) ? Items.SHIELD : replacedOffhandItem;
        if (item == null || !containsItem(item)) return null;
        return isTotemHandThreatPresent() ? null : item;
    }

    private boolean shouldFillOffhand(ItemStack offhand) {
        if (offhand.is(TOTEM)) return false;
        return shouldEquipForHealth() || offhand.isEmpty() && chooseRestoreItem(offhand) == null;
    }

    private boolean isSwapStillNeeded(Item desired) {
        if (!healthSwap.getValue() || OwnerSet.hasOwners()) return false;
        ItemStack offhand = client.player.getOffhandItem();
        if (desired == TOTEM) return shouldFillOffhand(offhand);
        return chooseRestoreItem(offhand) == desired;
    }

    private int findSwapSlot(Item item) {
        if (!healthSwap.getValue() || client.gui.screen() != null) return -1;
        if (isUsingMainHand() || isTotemHandProtecting()) return -1;
        if (item != TOTEM && client.player.getMainHandItem().is(item)) return selectedSlot();
        for (int slot = 0; slot < 9; ++slot) {
            if (slot == selectedSlot()) continue;
            if (client.player.getInventory().getItem(slot).is(item)) return slot;
        }
        return -1;
    }

    private boolean shouldEquipForHealth() {
        return client.player.getHealth() <= equipBelow.getValue();
    }

    private boolean shouldRestoreForHealth() {
        return client.player.getHealth() > Math.max(restoreAbove.getValue(), equipBelow.getValue());
    }

    private boolean hasUnselectedTotem() {
        for (int slot = 0; slot < 36; ++slot) {
            if (slot == selectedSlot()) continue;
            if (client.player.getInventory().getItem(slot).is(TOTEM)) return true;
        }
        return false;
    }

    private boolean containsItem(Item item) {
        for (int slot = 0; slot < 36; ++slot) {
            if (client.player.getInventory().getItem(slot).is(item)) return true;
        }
        return false;
    }

    private boolean isUsingMainHand() {
        return client.player.isUsingItem() && client.player.getUsedItemHand() == InteractionHand.MAIN_HAND;
    }

    private boolean isTotemHandProtecting() {
        TotemHandFeature feature = (TotemHandFeature) Echo.featureManager.findFeature(TotemHandFeature.class);
        return feature != null && feature.isProtectingMainHand();
    }

    private boolean isTotemHandThreatPresent() {
        TotemHandFeature feature = (TotemHandFeature) Echo.featureManager.findFeature(TotemHandFeature.class);
        return feature != null && feature.isExplosionThreatPresent();
    }

    private int selectedSlot() {
        return client.player.getInventory().getSelectedSlot();
    }

    private void resetState() {
        temporaryItemUse.reset();
        replacedOffhandItem = null;
        cachedDesiredItem = null;
        desiredItemTick = -1L;
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(AutoTotemFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoTotemFeature.refillHotbar);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoTotemFeature.slotToRefillTotem);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoTotemFeature.healthSwap);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoTotemFeature.equipBelow);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoTotemFeature.restoreAbove);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AutoTotemFeature.restoreItem);
    }

    /** Reads the shared restoreItemOption default without copying its value. */
    public static java.lang.CharSequence bootstrapRestoreItemOption() {
        return restoreItemOption;
    }

    /** Reads the shared restoreItemOption2 default without copying its value. */
    public static java.lang.CharSequence bootstrapRestoreItemOption2() {
        return restoreItemOption2;
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(AutoTotemFeature feature, dev.echo.client.setting.BooleanSetting preparedHealthSwap, dev.echo.client.setting.ChoiceSetting preparedRestoreItem, dev.echo.client.setting.IntSetting preparedSlotToRefillTotem, dev.echo.client.setting.IntSetting preparedRestoreAbove, dev.echo.client.setting.IntSetting preparedEquipBelow, dev.echo.client.setting.BooleanSetting preparedRefillHotbar) {
        AutoTotemFeature.healthSwap = preparedHealthSwap;
        AutoTotemFeature.restoreItem = preparedRestoreItem;
        AutoTotemFeature.slotToRefillTotem = preparedSlotToRefillTotem;
        AutoTotemFeature.restoreAbove = preparedRestoreAbove;
        AutoTotemFeature.equipBelow = preparedEquipBelow;
        AutoTotemFeature.refillHotbar = preparedRefillHotbar;
    }
}
