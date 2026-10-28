package dev.echo.client.features.impl.world;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.utils.ElapsedTimer;
import dev.echo.client.utils.inventory.SlotAction;
import dev.echo.client.utils.inventory.SlotPackets;
import dev.echo.client.utils.items.InventoryItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/** Ordered chest transfers preserving independent initial, transfer, and close timers. */
public class ChestStealerFeature extends Feature {
    public static IntSetting initialDelay;
    public static IntSetting takeDelay;
    public static IntSetting closeDelay;
    public static BooleanSetting onlyImportant;
    private dev.echo.client.utils.ElapsedTimer openDelayTimer;
    private dev.echo.client.utils.ElapsedTimer transferDelayTimer;
    private dev.echo.client.utils.ElapsedTimer closeDelayTimer;
    private boolean hasTransferredItems;

    public ChestStealerFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Chest Stealer"), dev.echo.client.bootstrap.CatalogText.literal("Automatically loots chests"), dev.echo.client.features.Category.WORLD));
    }

    @Override
    public void postInitialize() {
        openDelayTimer = new ElapsedTimer();
        transferDelayTimer = new ElapsedTimer();
        closeDelayTimer = new ElapsedTimer();
        hasTransferredItems = false;
        initialDelay.tooltip("Timing");
        takeDelay.tooltip("Timing");
        closeDelay.tooltip("Timing");
    }

    @Subscribe
    public void onClientTick(ClientTickEvent event) {
        if (client.player == null || client.world == null) {
            return;
        }
        if (!isEnabled()) {
            return;
        }
        ScreenHandler currentHandler = client.player.currentScreenHandler;
        if (!(currentHandler instanceof GenericContainerScreenHandler container)) {
            openDelayTimer.reset();
            transferDelayTimer.reset();
            closeDelayTimer.reset();
            hasTransferredItems = false;
            return;
        }
        if (!openDelayTimer.hasElapsedMillis(initialDelay.getValue())) {
            transferDelayTimer.reset();
            return;
        }
        if (hasNoEligibleItems(container)) {
            if (hasTransferredItems && closeDelayTimer.hasElapsedMillis(closeDelay.getValue())) {
                client.player.closeHandledScreen();
            }
            return;
        }

        int containerSize = container.getInventory().size();
        for (int slotIndex = 0; slotIndex < containerSize; slotIndex++) {
            Slot slot = container.getSlot(slotIndex);
            if (!slot.hasStack()) {
                continue;
            }
            ItemStack stack = slot.getStack();
            if (!isEligibleItem(stack)) {
                continue;
            }
            if (!transferDelayTimer.hasElapsedMillis(takeDelay.getValue())) {
                continue;
            }
            SlotPackets.clickSlot(client.player.currentScreenHandler.syncId, slotIndex,
                    0, SlotAction.QUICK_MOVE, client.player);
            hasTransferredItems = true;
            transferDelayTimer.reset();
            closeDelayTimer.reset();
        }
    }

    public boolean hasNoEligibleItems(GenericContainerScreenHandler container) {
        int containerSize = container.getInventory().size();
        for (int slotIndex = 0; slotIndex < containerSize; slotIndex++) {
            Slot slot = container.getSlot(slotIndex);
            if (slot.hasStack() && isEligibleItem(slot.getStack())) {
                return false;
            }
        }
        return true;
    }

    private boolean isEligibleItem(ItemStack stack) {
        if (!onlyImportant.getValue()) {
            return true;
        }
        return InventoryItems.isAxe(stack)
                || stack.contains(DataComponentTypes.FOOD)
                || stack.getItem() instanceof BlockItem
                || InventoryItems.isLegacySword(stack.getItem())
                || stack.contains(DataComponentTypes.WEAPON)
                || stack.contains(DataComponentTypes.TOOL)
                || stack.contains(DataComponentTypes.EQUIPPABLE)
                || stack.getItem() instanceof EnderPearlItem
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof ArrowItem
                || stack.getItem() instanceof FishingRodItem
                || stack.getItem() instanceof PotionItem;
    }

    @Override
    public void onDisable() {
        openDelayTimer.reset();
        transferDelayTimer.reset();
        closeDelayTimer.reset();
        hasTransferredItems = false;
        super.onDisable();
    }

}
