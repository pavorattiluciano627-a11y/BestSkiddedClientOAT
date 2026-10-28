package dev.echo.client.events.interaction;

import dev.echo.client.events.Event;
import dev.echo.client.events.EventPhase;
import dev.echo.client.events.input.KeybindingEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/** The player, hand, stack and target captured for an item-use hook. */
public abstract class ItemUseEvent extends Event {
    private final PlayerEntity player;
    private final Hand hand;
    private final ItemStack stack;
    private final HitResult hit;

    public ItemUseEvent(EventPhase phase, PlayerEntity player, Hand hand,
                        ItemStack stack, HitResult hit) {
        super(phase);
        this.player = player;
        this.hand = hand;
        this.stack = stack;
        this.hit = hit;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public Hand getHand() {
        return hand;
    }

    public ItemStack getStack() {
        return stack;
    }

    public HitResult getHit() {
        return hit;
    }

    public boolean isBlockHit() {
        return hit instanceof BlockHitResult;
    }

    public boolean isEntityHit() {
        return hit instanceof EntityHitResult;
    }

    /** A non-null shared sentinel bypasses the virtual predicate before casting. */
    public BlockHitResult getBlockHit() {
        return KeybindingEvent.getInputSentinel() != null || isBlockHit()
                ? (BlockHitResult) hit : null;
    }

    /** A non-null shared sentinel bypasses the virtual predicate before casting. */
    public EntityHitResult getEntityHit() {
        return KeybindingEvent.getInputSentinel() != null || isEntityHit()
                ? (EntityHitResult) hit : null;
    }

    /** Marker produced by the item-use entry hook. */
    public static class Pre extends ItemUseEvent {
        public Pre(PlayerEntity player, Hand hand, ItemStack stack, HitResult hit) {
            super(EventPhase.PRE, player, hand, stack, hit);
        }
    }

    /** Marker produced by the item-use return hook. */
    public static class Post extends ItemUseEvent {
        public Post(PlayerEntity player, Hand hand, ItemStack stack, HitResult hit) {
            super(EventPhase.POST, player, hand, stack, hit);
        }
    }
}
