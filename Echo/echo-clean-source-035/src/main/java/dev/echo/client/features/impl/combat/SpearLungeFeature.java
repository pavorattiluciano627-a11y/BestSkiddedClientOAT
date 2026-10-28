package dev.echo.client.features.impl.combat;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.events.interaction.AttackAttemptEvent;
import dev.echo.client.events.interaction.HandSwingEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.HotbarSlotsSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.setting.ItemListSetting;
import dev.echo.client.setting.KeybindSetting;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.targeting.WeaponAttributes;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;

/** Selects a Lunge-enchanted spear on an attack or an activation-key rising edge. */
public class SpearLungeFeature extends Feature {
    private static final CharSequence VANILLA = CatalogText.literal("Vanilla");
    private static final CharSequence ATTACK = CatalogText.literal("Attack");
    private static final CharSequence KEYPRESS = CatalogText.literal("Keypress");
    private static final CharSequence SLOT = CatalogText.literal("Slot");
    private static final CharSequence ITEM = CatalogText.literal("Item");
    private static final CharSequence PAPER = CatalogText.literal("Paper");
    private static ChoiceSetting mode;
    private static BooleanSetting silent;
    private static ChoiceSetting cooldown;
    private static IntSetting cooldownPercent;
    private static ItemListSetting itemWhitelist;
    private static HotbarSlotsSetting hotbarWhitelist;
    private static KeybindSetting activateKey;
    private static ChoiceSetting returnTo;
    private static HotbarSlotsSetting returnSlots;
    private static ItemListSetting returnItems;
    private boolean activationKeyWasPressed;
    private static String cooldownTooltip;
    private static String cooldownPercentTooltip;
    private static String itemWhitelistTooltip;
    private static String hotbarWhitelistTooltip;
    private static String returnToTooltip;
    private static String returnFiltersTooltip;

    public SpearLungeFeature() {
        super(new dev.echo.client.features.FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Spear Lunge"), dev.echo.client.bootstrap.CatalogText.literal("Uses the spear's lunge ability to move quickly"), dev.echo.client.features.Category.COMBAT));
    }

    @Override
    public void postInitialize() {
        activationKeyWasPressed = false;
        cooldownPercent.setVisibilityPredicate(ignored -> cooldown.isSelected(VANILLA));
        itemWhitelist.setVisibilityPredicate(ignored -> mode.isSelected(ATTACK));
        hotbarWhitelist.setVisibilityPredicate(ignored -> mode.isSelected(ATTACK));
        activateKey.setVisibilityPredicate(ignored -> mode.isSelected(KEYPRESS));
        returnSlots.setVisibilityPredicate(ignored -> returnTo.isSelected(SLOT));
        returnItems.setVisibilityPredicate(ignored -> returnTo.isSelected(ITEM));
        if (cooldownTooltip == null) cooldownTooltip = new String("Timing");
        cooldown.tooltip(cooldownTooltip);
        if (cooldownPercentTooltip == null) cooldownPercentTooltip = new String("Timing");
        cooldownPercent.tooltip(cooldownPercentTooltip);
        if (itemWhitelistTooltip == null) itemWhitelistTooltip = new String("Conditions");
        itemWhitelist.tooltip(itemWhitelistTooltip);
        if (hotbarWhitelistTooltip == null) hotbarWhitelistTooltip = new String("Conditions");
        hotbarWhitelist.tooltip(hotbarWhitelistTooltip);
        if (returnToTooltip == null) returnToTooltip = new String("Return");
        returnTo.tooltip(returnToTooltip);
        if (returnFiltersTooltip == null) returnFiltersTooltip = new String("Return");
        returnSlots.tooltip(returnFiltersTooltip);
        returnItems.tooltip(returnFiltersTooltip);
    }

    private int findLungeSpearSlot() {
        return InventoryItems.findItemWithEnchantmentInHotbar(
                stack -> InventoryItems.isSpear(stack.getItem()), Enchantments.LUNGE, false);
    }

    private boolean holdsLungeSpear() {
        if (client.player == null) return false;
        ItemStack stack = client.player.getMainHandStack();
        return !stack.isEmpty() && InventoryItems.isSpear(stack.getItem())
                && InventoryItems.hasEnchantment(stack, Enchantments.LUNGE);
    }

    @Subscribe
    public final void onAttackAttempt(AttackAttemptEvent event) {
        if (!mode.isSelected(ATTACK)) return;
        if (isOutsideWorld() || !isEnabled()) return;
        if (MinecraftRenderState.getScreen() != null || client.isPaused()) return;
        if (holdsLungeSpear()) return;
        if (!itemWhitelist.acceptsHeldItem() || !hotbarWhitelist.permitsCurrentSlot()) return;
        if (!tryActivate()) return;
        event.cancel();
        WeaponAttributes.performPiercingAttack();
    }

    @Subscribe
    public void onHandleInput(HandleInputEvent.Pre event) {
        if (!mode.isSelected(KEYPRESS)) return;
        if (isOutsideWorld() || !isEnabled()) return;
        if (MinecraftRenderState.getScreen() != null || client.isPaused()) return;
        boolean pressed = activateKey.getKey().isPressed();
        if (pressed && !activationKeyWasPressed) {
            if (holdsLungeSpear()) {
                activationKeyWasPressed = true;
                return;
            }
            if (tryActivate()) WeaponAttributes.performPiercingAttack();
        }
        activationKeyWasPressed = pressed;
    }

    private boolean tryActivate() {
        if (DeferredActionQueue.hasPendingActions()) return false;
        int spearSlot = findLungeSpearSlot();
        if (spearSlot == -1) return false;
        if (cooldown.isSelected(VANILLA)) {
            if (client.player.getAttackCooldownProgress(0.0f) < (float) cooldownPercent.getValue() / 100.0f) return false;
        } else if (cooldown.isSelected(PAPER)) {
            ItemStack spear = client.player.getInventory().getStack(spearSlot);
            if (WeaponAttributes.remainingCooldownSeconds(client.player, spear, 0.0f) > 0.0) return false;
        }
        if (client.player.getInventory().getSelectedSlot() == spearSlot) return false;
        return DeferredActionQueue.select(this, spearSlot, silent.getValue() ? 0 : 1, true, this::applyReturnSlot);
    }

    @Subscribe
    public void onHandSwing(HandSwingEvent event) {
        if (event.getPlayer() == client.player) WeaponAttributes.recordSwingTick(client.player);
    }

    private void applyReturnSlot() {
        int slot;
        if (returnTo.isSelected(SLOT)) {
            slot = returnSlots.chooseSelectedSlot();
        } else if (returnTo.isSelected(ITEM)) {
            slot = InventoryItems.findStackInHotbar(stack -> returnItems.isEntrySelected(stack.getItem()));
        } else {
            slot = -1;
        }
        if (slot != -1) InventoryItems.selectHotbarSlot(slot);
    }

    public ItemStack getPaperCooldownSpear() {
        if (!isEnabled() || !cooldown.isSelected(PAPER) || isOutsideWorld()) return ItemStack.EMPTY;
        int slot = findLungeSpearSlot();
        return slot == -1 ? ItemStack.EMPTY : client.player.getInventory().getStack(slot);
    }

    @Override
    public void onDisable() {
        DeferredActionQueue.finish(this);
        activationKeyWasPressed = false;
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(SpearLungeFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpearLungeFeature.mode);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpearLungeFeature.silent);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpearLungeFeature.cooldown);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpearLungeFeature.cooldownPercent);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpearLungeFeature.itemWhitelist);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpearLungeFeature.hotbarWhitelist);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpearLungeFeature.activateKey);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpearLungeFeature.returnTo);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpearLungeFeature.returnSlots);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpearLungeFeature.returnItems);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(SpearLungeFeature feature, dev.echo.client.setting.ChoiceSetting preparedCooldown, dev.echo.client.setting.KeybindSetting preparedActivateKey, dev.echo.client.setting.ChoiceSetting preparedMode, dev.echo.client.setting.ItemListSetting preparedReturnItems, dev.echo.client.setting.IntSetting preparedCooldownPercent, dev.echo.client.setting.HotbarSlotsSetting preparedHotbarWhitelist, dev.echo.client.setting.ItemListSetting preparedItemWhitelist, dev.echo.client.setting.BooleanSetting preparedSilent, dev.echo.client.setting.HotbarSlotsSetting preparedReturnSlots, dev.echo.client.setting.ChoiceSetting preparedReturnTo) {
        SpearLungeFeature.cooldown = preparedCooldown;
        SpearLungeFeature.activateKey = preparedActivateKey;
        SpearLungeFeature.mode = preparedMode;
        SpearLungeFeature.returnItems = preparedReturnItems;
        SpearLungeFeature.cooldownPercent = preparedCooldownPercent;
        SpearLungeFeature.hotbarWhitelist = preparedHotbarWhitelist;
        SpearLungeFeature.itemWhitelist = preparedItemWhitelist;
        SpearLungeFeature.silent = preparedSilent;
        SpearLungeFeature.returnSlots = preparedReturnSlots;
        SpearLungeFeature.returnTo = preparedReturnTo;
    }
}
