package dev.echo.client.events.interaction;

import dev.echo.client.events.Event;
import dev.echo.client.events.EventPhase;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Player and target references for the pre-attack and post-attack notifications.
 * This complete functional candidate retains the original analysis-only sentinel;
 * binding/cache applicability and its production removal remain separate obligations.
 */
public class EntityAttackEvent extends Event {
    private final Player player;
    private final Entity target;
    private static String[] attackSentinel;

    static {
        if (getAttackSentinel() != null) {
            setAttackSentinel(new String[5]);
        }
    }

    /**
     * Records the event phase and the original entity references.
     * @param phase attack stage
     * @param player attacking player
     * @param target entity being attacked
     */
    public EntityAttackEvent(EventPhase phase, Player player, Entity target) {
        super(phase);
        this.player = player;
        this.target = target;
    }

    /** @return the attacking player */
    public Player getPlayer() {
        return player;
    }

    /** @return the entity being attacked */
    public Entity getTarget() {
        return target;
    }

    /**
     * Retains the original static sentinel write in this analysis candidate.
     * @param sentinel replacement array reference, including {@code null}
     */
    public static void setAttackSentinel(String[] sentinel) {
        attackSentinel = sentinel;
    }

    /** @return the original shared sentinel reference without copying */
    public static String[] getAttackSentinel() {
        return attackSentinel;
    }

    /** Event emitted at the attack hook's head. */
    public static class Pre extends EntityAttackEvent {
        /**
         * Creates a pre-attack event.
         * @param player attacking player
         * @param target attacked entity
         */
        public Pre(Player player, Entity target) {
            super(EventPhase.PRE, player, target);
        }
    }

    /** Event emitted at the attack hook's tail. */
    public static class Post extends EntityAttackEvent {
        /**
         * Creates a post-attack event.
         * @param player attacking player
         * @param target attacked entity
         */
        public Post(Player player, Entity target) {
            super(EventPhase.POST, player, target);
        }
    }
}
