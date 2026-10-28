package dev.echo.client.events.interaction;

import dev.echo.client.events.Event;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/** Reports a player's requested hand swing before it is performed. */
public class HandSwingEvent extends Event {
    private final Player player;
    private final InteractionHand hand;

    /**
     * Records the player and hand without copying or validating either reference.
     * @param player player requesting the swing
     * @param hand hand being swung
     */
    public HandSwingEvent(Player player, InteractionHand hand) {
        this.player = player;
        this.hand = hand;
    }

    /** @return the player requesting the swing */
    public Player getPlayer() {
        return player;
    }

    /** @return the hand being swung */
    public InteractionHand getHand() {
        return hand;
    }
}
