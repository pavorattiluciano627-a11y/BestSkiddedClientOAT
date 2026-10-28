package dev.echo.client.events.world;

import dev.echo.client.events.Event;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/** A replaceable hitbox for an immutable entity reference. No normalization is done. */
public class EntityHitboxEvent extends Event {
    private final Entity entity;
    private AABB boundingBox;

    public EntityHitboxEvent(Entity entity, AABB boundingBox) {
        this.entity = entity;
        this.boundingBox = boundingBox;
    }

    public Entity getEntity() {
        return entity;
    }

    public AABB getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(AABB boundingBox) {
        this.boundingBox = boundingBox;
    }
}
