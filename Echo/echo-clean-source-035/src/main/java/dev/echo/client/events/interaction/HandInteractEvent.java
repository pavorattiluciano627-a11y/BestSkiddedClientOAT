package dev.echo.client.events.interaction;

import dev.echo.client.events.Event;
import dev.echo.client.events.EventPhase;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/** Interaction payload retains the player's identity and the selected hand. */
public class HandInteractEvent extends Event {
    private final Player player;
    private final InteractionHand hand;

    public HandInteractEvent(EventPhase phase, Player player, InteractionHand hand) {
        super(phase);
        this.player = player;
        this.hand = hand;
    }

    public Player getPlayer() {
        return player;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public static class Pre extends HandInteractEvent {
        public Pre(Player player, InteractionHand hand) {
            super(EventPhase.PRE, player, hand);
        }
    }
}
