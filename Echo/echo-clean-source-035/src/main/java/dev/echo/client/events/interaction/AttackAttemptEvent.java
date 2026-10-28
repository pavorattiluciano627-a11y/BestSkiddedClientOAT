package dev.echo.client.events.interaction;

import dev.echo.client.events.Event;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Attack attempt payload. Analysis candidate: accessor linkage awaits key proof. */
public class AttackAttemptEvent extends Event {
    private final Player player;
    private final Entity target;

    /** Captures the attacking player and the possibly absent entity target. */
    public AttackAttemptEvent(Player player, Entity target) {
        this.player = player;
        this.target = target;
    }

    /** Returns the attacking player. */
    public Player getPlayer() { return player; }

    /** Returns the target, or null when the producer has no entity target. */
    public Entity getTarget() { return target; }
}
