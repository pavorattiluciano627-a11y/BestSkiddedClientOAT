package dev.echo.client.features.impl.utility;

import dev.echo.client.bootstrap.CatalogSettings;
import net.minecraft.world.inventory.ContainerInput;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.input.InputKey;
import dev.echo.client.mixin.HandledScreenAccessor;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.utils.ActionTimer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

/** Inventory potion refill. Conditional source domain and ordering receipts accompany this file. */
public class PotRefillFeature extends Feature {
    private static final CharSequence HOVER = CatalogText.literal("Hover");
    private static final CharSequence SHIFT_LEFT_CLICK = CatalogText.literal("ShiftLMB");
    private static final CharSequence AUTO = CatalogText.literal("Auto");
    private static ChoiceSetting mode;
    private static BooleanSetting refillBuffPotions;
    private static FloatRangeSetting delay;
    private ActionTimer refillTimer;

    public PotRefillFeature() {
        super(new FeatureInfo(CatalogText.literal("Pot Refill"),
                CatalogText.literal("Automatically refills potions"), Category.UTILITY));
    }

    public static void appendBootstrapSettings(PotRefillFeature feature) {
        CatalogSettings.append(feature, mode);
        CatalogSettings.append(feature, refillBuffPotions);
        CatalogSettings.append(feature, delay);
    }

    public static CharSequence bootstrapModeOption3() { return HOVER; }
    public static CharSequence bootstrapModeOption() { return SHIFT_LEFT_CLICK; }
    public static CharSequence bootstrapModeOption2() { return AUTO; }

    public static void assignBootstrapSettings(PotRefillFeature feature, ChoiceSetting preparedMode,
            BooleanSetting preparedRefillBuffPotions, FloatRangeSetting preparedDelay) {
        mode = preparedMode;
        refillBuffPotions = preparedRefillBuffPotions;
        delay = preparedDelay;
    }

    @Override
    public void postInitialize() {
        refillTimer = new ActionTimer();
        refillBuffPotions.tooltip("Appearance");
        delay.tooltip("Timing");
    }

    @Subscribe
    private void onTick(ClientTickEvent event) {
        if (isOutsideWorld()) return;
        if (!(Minecraft.getInstance().gui.screen() instanceof InventoryScreen inventory)) return;
        if (mode.isSelected(HOVER)) {
            refillHovered(inventory);
        } else if (mode.isSelected(SHIFT_LEFT_CLICK)) {
            refillShiftClicked(inventory);
        } else if (mode.isSelected(AUTO)) {
            refillAutomatically(inventory);
        }
    }

    private void refillHovered(InventoryScreen inventory) {
        Slot slot = ((HandledScreenAccessor) inventory).getFocusedSlot();
        if (slot == null) return;
        ItemStack stack = slot.getItem();
        if (!shouldRefill(stack)) return;
        if (isHotbarIndex(slot.getContainerSlot())) return;
        int destination = findEmptyHotbarSlot();
        if (destination == -1) return;
        if (!refillTimer.hasElapsedMillis((double) delay.sampleGaussian())) return;
        refillTimer.reset();
        // Preserve getIndex(), rather than substituting Slot.id.
        swapIntoHotbar(inventory.getMenu().containerId, slot.getContainerSlot(), destination, client.player);
    }

    private void refillShiftClicked(InventoryScreen inventory) {
        Slot slot = ((HandledScreenAccessor) inventory).getFocusedSlot();
        if (slot == null) return;
        if (!InputKey.LEFT_SHIFT.isPressed()) return;
        if (!InputKey.MOUSE_LEFT.isPressed()) return;
        ItemStack stack = slot.getItem();
        if (!shouldRefill(stack)) return;
        if (isHotbarIndex(slot.getContainerSlot())) return;
        int destination = findEmptyHotbarSlot();
        if (destination == -1) return;
        if (!refillTimer.hasElapsedMillis((double) delay.sampleGaussian())) return;
        refillTimer.reset();
        swapIntoHotbar(inventory.getMenu().containerId, slot.getContainerSlot(), destination, client.player);
    }

    private void refillAutomatically(InventoryScreen inventory) {
        if (!refillTimer.hasElapsedMillis((double) delay.sampleGaussian())) return;
        refillTimer.reset();
        for (int slot = 9; slot < 45; ++slot) {
            ItemStack stack = inventory.getMenu().getSlot(slot).getItem();
            if (!shouldRefill(stack)) continue;
            int destination = findEmptyHotbarSlot();
            if (destination == -1) continue;
            swapIntoHotbar(inventory.getMenu().containerId, slot, destination, client.player);
            break;
        }
    }

    private boolean isSplashPotion(ItemStack stack) {
        return stack.getItem() == Items.SPLASH_POTION;
    }

    private boolean shouldRefill(ItemStack stack) {
        if (!isSplashPotion(stack) || client.player == null) return false;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;
        for (MobEffectInstance instance : contents.getAllEffects()) {
            Holder<MobEffect> entry = instance.getEffect();
            MobEffect effect = entry.value();
            if (isInstant(effect) && effect.isBeneficial()) return true;
            if (!refillBuffPotions.getValue()) continue;
            if (effect.isBeneficial() && !isInstant(effect)
                    && !client.player.hasEffect(entry) && !hotbarHasEffect(entry)) return true;
        }
        return false;
    }

    private boolean hotbarHasEffect(Holder<MobEffect> effect) {
        for (int slot = 0; slot < 9; ++slot) {
            if (containsBuffEffect(client.player.getInventory().getItem(slot), effect)) return true;
        }
        return false;
    }

    private boolean containsBuffEffect(ItemStack stack, Holder<MobEffect> wanted) {
        if (!isSplashPotion(stack)) return false;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;
        for (MobEffectInstance instance : contents.getAllEffects()) {
            Holder<MobEffect> entry = instance.getEffect();
            MobEffect effect = entry.value();
            if (effect.isBeneficial() && !isInstant(effect) && entry.equals(wanted)) return true;
        }
        return false;
    }

    private boolean isHotbarIndex(int slot) { return slot >= 0 && slot <= 8; }

    private int findEmptyHotbarSlot() {
        for (int slot = 0; slot < 9; ++slot) {
            if (client.player.getInventory().getItem(slot).isEmpty()) return slot;
        }
        return -1;
    }

    private boolean isInstant(MobEffect effect) { return effect.isInstantaneous(); }

    /** Inlined SWAP specialization of the original SlotPackets.createConnection helper. */
    private static void swapIntoHotbar(int syncId, int source, int destination, Player player) {
        if (player == null) return;
        if (client.gameMode == null) return;
        client.gameMode.handleContainerInput(syncId, source, destination, ContainerInput.SWAP, player);
    }
}
