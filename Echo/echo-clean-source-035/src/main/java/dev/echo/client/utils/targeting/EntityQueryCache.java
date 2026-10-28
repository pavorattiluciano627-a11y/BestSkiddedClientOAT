package dev.echo.client.utils.targeting;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;

/**
 * Concrete weak-entity contact-time cache. Times and intervals retain their
 * original signed arithmetic; binding/control applicability is tracked separately.
 */
public final class EntityQueryCache {
    private final Map<Entity, Long> lastQueryTimes = new WeakHashMap<>();

    public EntityQueryCache() {
    }

    /** Read-only test: neither expires an entry nor refreshes its timestamp. */
    public boolean wasQueriedRecently(long now, Entity entity, int interval) {
        Long previous = lastQueryTimes.get(entity);
        return previous != null && now - previous < interval;
    }

    public void recordQuery(long time, Entity entity) {
        lastQueryTimes.put(entity, time);
    }

    public void clear() {
        lastQueryTimes.clear();
    }
}
