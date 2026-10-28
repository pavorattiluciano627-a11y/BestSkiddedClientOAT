package dev.echo.client.events.interaction;

import dev.echo.client.events.Event;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/** Attack attempt payload. Analysis candidate: accessor linkage awaits key proof. */
public class AttackAttemptEvent extends Event {
    private final PlayerEntity player;
    private final Entity target;

    /** Captures the attacking player and the possibly absent entity target. */
    public AttackAttemptEvent(PlayerEntity player, Entity target) {
        this.player = player;
        this.target = target;
    }

    /** Returns the attacking player. */
    public PlayerEntity getPlayer() { return player; }

    /** Returns the target, or null when the producer has no entity target. */
    public Entity getTarget() { return target; }
}
