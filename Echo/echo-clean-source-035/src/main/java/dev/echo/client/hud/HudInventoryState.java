package dev.echo.client.hud;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * The original HudRenderer's live player inventory references and selection state.
 * This extraction intentionally does not copy stacks or normalize invalid indices.
 * The original cache, key and sentinel applicability is retained in completion-v3.
 */
public final class HudInventoryState {
    private static final EquipmentSlot[] ARMOR_ORDER = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private final ItemStack[] hotbar = new ItemStack[9];
    private final ItemStack[] armor = new ItemStack[4];
    private ItemStack offhand = ItemStack.EMPTY;
    private int selectedSlot;
    private float attackCooldown = 1.0f;
    private PlayerEntity player;

    /** Initializes every original stack slot with the shared EMPTY object. */
    public HudInventoryState() {
        reset();
    }

    /** Reads current stacks in original hotbar, selected-slot, armor, offhand order. */
    public void update(PlayerEntity player) {
        this.player = player;
        if (player == null) {
            reset();
            return;
        }
        for (int slot = 0; slot < 9; slot++) {
            hotbar[slot] = player.getInventory().getStack(slot);
        }
        selectedSlot = Math.max(0, Math.min(8, player.getInventory().getSelectedSlot()));
        for (int slot = 0; slot < 4; slot++) {
            armor[slot] = player.getEquippedStack(ARMOR_ORDER[slot]);
        }
        offhand = player.getOffHandStack();
    }

    /** Resets the inventory portion of the HUD to its original no-player defaults. */
    public void reset() {
        for (int slot = 0; slot < 9; slot++) hotbar[slot] = ItemStack.EMPTY;
        for (int slot = 0; slot < 4; slot++) armor[slot] = ItemStack.EMPTY;
        offhand = ItemStack.EMPTY;
        selectedSlot = 0;
        attackCooldown = 1.0f;
        player = null;
    }

    /** Returns the retained hotbar stack reference. */
    public ItemStack hotbar(int slot) { return hotbar[slot]; }
    /** Returns the retained armor stack reference in head-to-feet order. */
    public ItemStack armor(int slot) { return armor[slot]; }
    /** Returns the retained offhand stack reference. */
    public ItemStack offhand() { return offhand; }
    /** Returns the bounded selected hotbar slot. */
    public int selectedSlot() { return selectedSlot; }
    /** Returns the player whose stacks are currently retained. */
    public PlayerEntity player() { return player; }
    /** Returns the original attack-indicator state. */
    public float attackCooldown() { return attackCooldown; }
    /** Stores the attack-indicator value calculated by the owning frame update. */
    public void setAttackCooldown(float value) { attackCooldown = value; }
}
