package dev.echo.client.scripting.api;

import dev.echo.client.events.interaction.AttackAttemptEvent;
import net.minecraft.entity.Entity;

/** Script-facing attacker, nullable target, and cancellation for an attack attempt. */
public class AttackEventView extends EventView {
    private final AttackAttemptEvent attackEvent;

    /** Retains the attack event used by the dispatching hook. */
    public AttackEventView(AttackAttemptEvent event) {
        super(event);
        attackEvent = event;
    }

    /** Allocates a fresh player wrapper; the original did not null-check the attacker. */
    public PlayerView getPlayer() { return new PlayerView(attackEvent.getPlayer()); }

    /** Returns null for an absent target, otherwise a fresh entity wrapper. */
    public EntityView getTarget() {
        Entity target = attackEvent.getTarget();
        return target == null ? null : new EntityView(target);
    }
}
