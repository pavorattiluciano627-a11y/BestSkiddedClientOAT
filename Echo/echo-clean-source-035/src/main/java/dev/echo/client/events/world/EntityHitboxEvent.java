package dev.echo.client.events.world;

import dev.echo.client.events.Event;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

/** A replaceable hitbox for an immutable entity reference. No normalization is done. */
public class EntityHitboxEvent extends Event {
    private final Entity entity;
    private Box boundingBox;

    public EntityHitboxEvent(Entity entity, Box boundingBox) {
        this.entity = entity;
        this.boundingBox = boundingBox;
    }

    public Entity getEntity() {
        return entity;
    }

    public Box getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(Box boundingBox) {
        this.boundingBox = boundingBox;
    }
}
