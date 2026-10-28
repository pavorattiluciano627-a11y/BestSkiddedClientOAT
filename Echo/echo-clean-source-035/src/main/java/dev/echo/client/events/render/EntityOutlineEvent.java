package dev.echo.client.events.render;

import dev.echo.client.events.Event;
import net.minecraft.entity.Entity;

/** Mutable outline decision, distinct from the inherited cancellation flag. */
public class EntityOutlineEvent extends Event {
    private final Entity entity;
    private boolean outlined;

    public EntityOutlineEvent(Entity entity, boolean outlined) {
        this.entity = entity;
        this.outlined = outlined;
    }

    public Entity getEntity() {
        return entity;
    }

    public boolean isOutlined() {
        return outlined;
    }

    public void setOutlined(boolean outlined) {
        this.outlined = outlined;
    }
}
