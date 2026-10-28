package dev.echo.client.events.render;

import dev.echo.client.events.Event;
import dev.echo.client.events.EventBus;
import java.util.UUID;
import net.minecraft.entity.player.SkinTextures;

/** Allows subscribers to replace the skin textures selected for a player. */
public class SkinTexturesEvent extends Event {
    /** UUID of the player whose textures are being queried. */
    public final UUID playerId;
    /** Selected textures, replaceable by subscribers. */
    public SkinTextures textures;

    private SkinTexturesEvent(UUID playerId, SkinTextures textures) {
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
    public static SkinTextures resolve(UUID playerId, SkinTextures textures) {
        if (!EventBus.hasSubscribers(SkinTexturesEvent.class)) {
            return textures;
        }
        SkinTexturesEvent event = new SkinTexturesEvent(playerId, textures);
        event.post();
        return event.textures;
    }
}
