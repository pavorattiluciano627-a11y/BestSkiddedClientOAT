package dev.echo.client.utils.items;

import dev.echo.client.scripting.api.ItemStackView;
import dev.echo.client.utils.inventory.SlotAction;
import dev.echo.client.utils.inventory.SlotPackets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;

/**
 * Live inventory state and screen-slot swaps exposed through the player script view.
 * Recovery applicability is recorded in the accompanying provider receipt.
 */
public class PlayerInventoryView {
    private final PlayerInventory inventory;

    /**
     * Retains the supplied inventory without validation or local-player substitution.
     *
     * @param inventory the inventory to wrap, including null
     */
    public PlayerInventoryView(PlayerInventory inventory) {
        this.inventory = inventory;
    }

    /** Returns a fresh view of the stack at the original, unmodified inventory index. */
    public ItemStackView getStack(int slot) {
        return new ItemStackView(inventory.getStack(slot));
    }

    /** Returns the wrapped inventory's current size. */
    public int size() {
        return inventory.size();
    }

    /** Returns the wrapped inventory's current selected slot. */
    public int getSelectedSlot() {
        return inventory.getSelectedSlot();
    }

    /** Returns a fresh view of the stack read using the current selected slot. */
    public ItemStackView getSelectedStack() {
        return new ItemStackView(inventory.getStack(inventory.getSelectedSlot()));
    }

    /**
     * Swaps a mapped inventory slot through the current local player's screen handler.
     *
     * @param inventorySlot the inventory index, preserving negative and out-of-range inputs
     * @param button the original swap button integer, passed through unchanged
     */
    public void swap(int inventorySlot, int button) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager == null || client.player == null) {
            return;
        }
        PlayerScreenHandler screenHandler = client.player.playerScreenHandler;
        SlotPackets.clickSlot(
                screenHandler.syncId, toScreenSlot(inventorySlot), button, SlotAction.SWAP, client.player);
    }

    /** Maps every signed integer using the original player-screen slot layout. */
    private int toScreenSlot(int slot) {
        if (slot <= 8) {
            return 36 + slot;
        }
        if (slot <= 35) {
            return slot;
        }
        if (slot <= 39) {
            return 44 - slot;
        }
        return 45;
    }
}
