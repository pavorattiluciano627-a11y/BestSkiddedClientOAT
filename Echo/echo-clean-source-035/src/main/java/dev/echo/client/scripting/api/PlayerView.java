package dev.echo.client.scripting.api;

import dev.echo.client.utils.items.PlayerInventoryView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/** Live player state and actions exposed to scripts through an EntityView subtype. */
public class PlayerView extends EntityView {
    private final PlayerEntity player;

    /**
     * Retains the supplied player in both the entity and player views.
     *
     * @param player the player to wrap, without a new null check or local-player substitution
     */
    public PlayerView(PlayerEntity player) {
        super(player);
        this.player = player;
    }

    /** Returns a fresh wrapper for the player's current velocity. */
    public Vector3View getVelocity() { return new Vector3View(player.getVelocity()); }

    /** Returns the player's current grounded state. */
    public boolean isOnGround() { return player.isOnGround(); }

    /** Returns the player's health. */
    public float getHealth() { return player.getHealth(); }

    /** Returns whether the player is sprinting. */
    public boolean isSprinting() { return player.isSprinting(); }

    /** Returns whether the player is using an item. */
    public boolean isUsingItem() { return player.isUsingItem(); }

    /** Returns whether the player is touching water. */
    public boolean isTouchingWater() { return player.isTouchingWater(); }

    /** Returns the player's dead-state predicate, not the negation of isAlive. */
    public boolean isDead() { return player.isDead(); }

    /** Returns whether the player is gliding. */
    public boolean isGliding() { return player.isGliding(); }

    /** Returns the player's creative-mode predicate. */
    public boolean isCreative() { return player.isCreative(); }

    /** Returns whether the player is sneaking. */
    public boolean isSneaking() { return player.isSneaking(); }

    /** Samples attack cooldown at the original half-tick argument. */
    public float getAttackCooldownProgress() { return player.getAttackCooldownProgress(0.5F); }

    /** Returns block-interaction range, not entity-interaction range. */
    public double getBlockInteractionRange() { return player.getBlockInteractionRange(); }

    /** Returns a fresh wrapper around this player's inventory. */
    public PlayerInventoryView getInventory() { return new PlayerInventoryView(player.getInventory()); }

    /**
     * Swings the selected hand; only case-insensitive "off" selects the off hand.
     *
     * @param hand the original script hand string; null and all other strings select main hand
     */
    public void swingHand(String hand) { player.swingHand(parseHand(hand)); }

    /**
     * Updates yaw before pitch, without adding normalization or clamping.
     *
     * @param yaw the new yaw
     * @param pitch the new pitch
     */
    public void setRotation(float yaw, float pitch) {
        player.setYaw(yaw);
        player.setPitch(pitch);
    }

    /** Sets sprinting on the wrapped player. */
    public void setSprinting(boolean sprinting) { player.setSprinting(sprinting); }

    /**
     * Delegates the inventory selection to PlayerInventory.setSelectedSlot.
     *
     * @param slot the original integer, without screen-slot remapping or added validation
     */
    public void setSelectedSlot(int slot) { player.getInventory().setSelectedSlot(slot); }

    /** Sets velocity from a newly allocated vector. */
    public void setVelocity(double x, double y, double z) { player.setVelocity(new Vec3d(x, y, z)); }

    /** Clears active item use on the wrapped player. */
    public void clearActiveItem() { player.clearActiveItem(); }

    /** Interacts using the wrapped player and main hand when an interaction manager exists. */
    public void interactMainHand() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager == null) return;
        client.interactionManager.interactItem(player, Hand.MAIN_HAND);
    }

    /**
     * Attacks the target's retained entity using the wrapped player.
     *
     * @param target the target view; it is accessed only after the interaction-manager guard
     */
    public void attack(EntityView target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager == null) return;
        client.interactionManager.attackEntity(player, target.entity);
    }

    private Hand parseHand(String hand) {
        return "off".equalsIgnoreCase(hand) ? Hand.OFF_HAND : Hand.MAIN_HAND;
    }
}
