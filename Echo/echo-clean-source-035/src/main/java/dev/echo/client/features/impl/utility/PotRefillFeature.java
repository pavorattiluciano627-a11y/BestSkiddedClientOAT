package dev.echo.client.features.impl.utility;

import dev.echo.client.bootstrap.CatalogSettings;
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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

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
        if (!(MinecraftClient.getInstance().currentScreen instanceof InventoryScreen inventory)) return;
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
        ItemStack stack = slot.getStack();
        if (!shouldRefill(stack)) return;
        if (isHotbarIndex(slot.getIndex())) return;
        int destination = findEmptyHotbarSlot();
        if (destination == -1) return;
        if (!refillTimer.hasElapsedMillis((double) delay.sampleGaussian())) return;
        refillTimer.reset();
        // Preserve getIndex(), rather than substituting Slot.id.
        swapIntoHotbar(inventory.getScreenHandler().syncId, slot.getIndex(), destination, client.player);
    }

    private void refillShiftClicked(InventoryScreen inventory) {
        Slot slot = ((HandledScreenAccessor) inventory).getFocusedSlot();
        if (slot == null) return;
        if (!InputKey.LEFT_SHIFT.isPressed()) return;
        if (!InputKey.MOUSE_LEFT.isPressed()) return;
        ItemStack stack = slot.getStack();
        if (!shouldRefill(stack)) return;
        if (isHotbarIndex(slot.getIndex())) return;
        int destination = findEmptyHotbarSlot();
        if (destination == -1) return;
        if (!refillTimer.hasElapsedMillis((double) delay.sampleGaussian())) return;
        refillTimer.reset();
        swapIntoHotbar(inventory.getScreenHandler().syncId, slot.getIndex(), destination, client.player);
    }

    private void refillAutomatically(InventoryScreen inventory) {
        if (!refillTimer.hasElapsedMillis((double) delay.sampleGaussian())) return;
        refillTimer.reset();
        for (int slot = 9; slot < 45; ++slot) {
            ItemStack stack = inventory.getScreenHandler().getSlot(slot).getStack();
            if (!shouldRefill(stack)) continue;
            int destination = findEmptyHotbarSlot();
            if (destination == -1) continue;
            swapIntoHotbar(inventory.getScreenHandler().syncId, slot, destination, client.player);
            break;
        }
    }

    private boolean isSplashPotion(ItemStack stack) {
        return stack.getItem() == Items.SPLASH_POTION;
    }

    private boolean shouldRefill(ItemStack stack) {
        if (!isSplashPotion(stack) || client.player == null) return false;
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return false;
        for (StatusEffectInstance instance : contents.getEffects()) {
            RegistryEntry<StatusEffect> entry = instance.getEffectType();
            StatusEffect effect = entry.value();
            if (isInstant(effect) && effect.isBeneficial()) return true;
            if (!refillBuffPotions.getValue()) continue;
            if (effect.isBeneficial() && !isInstant(effect)
                    && !client.player.hasStatusEffect(entry) && !hotbarHasEffect(entry)) return true;
        }
        return false;
    }

    private boolean hotbarHasEffect(RegistryEntry<StatusEffect> effect) {
        for (int slot = 0; slot < 9; ++slot) {
            if (containsBuffEffect(client.player.getInventory().getStack(slot), effect)) return true;
        }
        return false;
    }

    private boolean containsBuffEffect(ItemStack stack, RegistryEntry<StatusEffect> wanted) {
        if (!isSplashPotion(stack)) return false;
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return false;
        for (StatusEffectInstance instance : contents.getEffects()) {
            RegistryEntry<StatusEffect> entry = instance.getEffectType();
            StatusEffect effect = entry.value();
            if (effect.isBeneficial() && !isInstant(effect) && entry.equals(wanted)) return true;
        }
        return false;
    }

    private boolean isHotbarIndex(int slot) { return slot >= 0 && slot <= 8; }

    private int findEmptyHotbarSlot() {
        for (int slot = 0; slot < 9; ++slot) {
            if (client.player.getInventory().getStack(slot).isEmpty()) return slot;
        }
        return -1;
    }

    private boolean isInstant(StatusEffect effect) { return effect.isInstant(); }

    /** Inlined SWAP specialization of the original SlotPackets.createConnection helper. */
    private static void swapIntoHotbar(int syncId, int source, int destination, PlayerEntity player) {
        if (player == null) return;
        if (client.interactionManager == null) return;
        client.interactionManager.clickSlot(syncId, source, destination, SlotActionType.SWAP, player);
    }
}
