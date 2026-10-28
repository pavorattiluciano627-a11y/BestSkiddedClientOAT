package dev.echo.client.events.interaction;

import dev.echo.client.events.Event;
import dev.echo.client.events.EventPhase;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

/** Interaction payload retains the player's identity and the selected hand. */
public class HandInteractEvent extends Event {
    private final PlayerEntity player;
    private final Hand hand;

    public HandInteractEvent(EventPhase phase, PlayerEntity player, Hand hand) {
        super(phase);
        this.player = player;
        this.hand = hand;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public Hand getHand() {
        return hand;
    }

    public static class Pre extends HandInteractEvent {
        public Pre(PlayerEntity player, Hand hand) {
            super(EventPhase.PRE, player, hand);
        }
    }
}
