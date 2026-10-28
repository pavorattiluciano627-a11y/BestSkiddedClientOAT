package dev.echo.client.events.interaction;

import dev.echo.client.events.Event;
import dev.echo.client.events.EventPhase;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/** The player, hand and target captured by a block-interaction hook. */
public class BlockInteractEvent extends Event {
    private final Player player;
    private final InteractionHand hand;
    private final BlockHitResult hit;

    public BlockInteractEvent(EventPhase phase, Player player, InteractionHand hand, BlockHitResult hit) {
        super(phase);
        this.player = player;
        this.hand = hand;
        this.hit = hit;
    }

    public Player getPlayer() {
        return player;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public BlockHitResult getHit() {
        return hit;
    }

    /** Marker produced by the block-interaction entry hook. */
    public static class Pre extends BlockInteractEvent {
        public Pre(Player player, InteractionHand hand, BlockHitResult hit) {
            super(EventPhase.PRE, player, hand, hit);
        }
    }

    /** Marker produced by the block-interaction return hook. */
    public static class Post extends BlockInteractEvent {
        public Post(Player player, InteractionHand hand, BlockHitResult hit) {
            super(EventPhase.POST, player, hand, hit);
        }
    }
}
