package dev.echo.client.events.interaction;

import dev.echo.client.events.Event;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

/** Reports a player's requested hand swing before it is performed. */
public class HandSwingEvent extends Event {
    private final PlayerEntity player;
    private final Hand hand;

    /**
     * Records the player and hand without copying or validating either reference.
     * @param player player requesting the swing
     * @param hand hand being swung
     */
    public HandSwingEvent(PlayerEntity player, Hand hand) {
        this.player = player;
        this.hand = hand;
    }

    /** @return the player requesting the swing */
    public PlayerEntity getPlayer() {
        return player;
    }

    /** @return the hand being swung */
    public Hand getHand() {
        return hand;
    }
}
