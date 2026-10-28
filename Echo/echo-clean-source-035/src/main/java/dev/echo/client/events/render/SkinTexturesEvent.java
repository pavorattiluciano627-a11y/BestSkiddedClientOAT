package dev.echo.client.events.render;

import dev.echo.client.events.Event;
import dev.echo.client.events.EventBus;
import java.util.UUID;
import net.minecraft.world.entity.player.PlayerSkin;

/** Allows subscribers to replace the skin textures selected for a player. */
public class SkinTexturesEvent extends Event {
    /** UUID of the player whose textures are being queried. */
    public final UUID playerId;
    /** Selected textures, replaceable by subscribers. */
    public PlayerSkin textures;

    private SkinTexturesEvent(UUID playerId, PlayerSkin textures) {
        this.playerId = playerId;
        this.textures = textures;
    }

    /**
     * Queries subscribers only when at least one is registered for this event.
     * Cancellation does not discard a subscriber's selected textures.
     * @param playerId player whose skin is being selected
     * @param textures initially selected textures
     * @return the selected textures after synchronous event delivery
     */
    public static PlayerSkin resolve(UUID playerId, PlayerSkin textures) {
        if (!EventBus.hasSubscribers(SkinTexturesEvent.class)) {
            return textures;
        }
        SkinTexturesEvent event = new SkinTexturesEvent(playerId, textures);
        event.post();
        return event.textures;
    }
}
