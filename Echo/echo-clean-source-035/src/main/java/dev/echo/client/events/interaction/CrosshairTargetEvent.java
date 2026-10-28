package dev.echo.client.events.interaction;

import dev.echo.client.events.Event;
import net.minecraft.util.hit.HitResult;

/** Allows subscribers to replace the result of the crosshair-target query. */
public class CrosshairTargetEvent extends Event {
    /** Query result, which subscribers may replace, including with {@code null}. */
    public HitResult target;

    /**
     * Records the result returned by the original query.
     * @param target original crosshair target
     */
    public CrosshairTargetEvent(HitResult target) {
        this.target = target;
    }
}
