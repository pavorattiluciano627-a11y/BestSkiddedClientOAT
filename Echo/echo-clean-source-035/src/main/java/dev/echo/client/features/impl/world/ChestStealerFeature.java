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
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;

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
        if (client.player == null || client.level == null) {
            return;
        }
        if (!isEnabled()) {
            return;
        }
        AbstractContainerMenu currentHandler = client.player.containerMenu;
        if (!(currentHandler instanceof ChestMenu container)) {
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
                client.player.closeContainer();
            }
            return;
        }

        int containerSize = container.getContainer().getContainerSize();
        for (int slotIndex = 0; slotIndex < containerSize; slotIndex++) {
            Slot slot = container.getSlot(slotIndex);
            if (!slot.hasItem()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (!isEligibleItem(stack)) {
                continue;
            }
            if (!transferDelayTimer.hasElapsedMillis(takeDelay.getValue())) {
                continue;
            }
            SlotPackets.clickSlot(client.player.containerMenu.containerId, slotIndex,
                    0, SlotAction.QUICK_MOVE, client.player);
            hasTransferredItems = true;
            transferDelayTimer.reset();
            closeDelayTimer.reset();
        }
    }

    public boolean hasNoEligibleItems(ChestMenu container) {
        int containerSize = container.getContainer().getContainerSize();
        for (int slotIndex = 0; slotIndex < containerSize; slotIndex++) {
            Slot slot = container.getSlot(slotIndex);
            if (slot.hasItem() && isEligibleItem(slot.getItem())) {
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
                || stack.has(DataComponents.FOOD)
                || stack.getItem() instanceof BlockItem
                || InventoryItems.isLegacySword(stack.getItem())
                || stack.has(DataComponents.WEAPON)
                || stack.has(DataComponents.TOOL)
                || stack.has(DataComponents.EQUIPPABLE)
                || stack.getItem() instanceof EnderpearlItem
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
