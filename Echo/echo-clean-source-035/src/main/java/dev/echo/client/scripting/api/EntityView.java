package dev.echo.client.scripting.api;

import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;

public class EntityView {
    protected final Entity entity;

    public EntityView(Entity entity) {
        this.entity = entity;
    }

    public Vector3View getPosition() {
        return new Vector3View(entity.getEntityPos());
    }

    public Vector3View getEyePosition() {
        return new Vector3View(entity.getEyePos());
    }

    public float getYaw() {
        return entity.getYaw();
    }

    public float getPitch() {
        return entity.getPitch();
    }

    public boolean isAlive() {
        return entity.isAlive();
    }

    public int getId() {
        return entity.getId();
    }

    public int getAge() {
        return entity.age;
    }

    public String getName() {
        return entity.getName().getString();
    }

    public String getDisplayName() {
        return entity.getDisplayName().getString();
    }

    public String getTypeId() {
        return Registries.ENTITY_TYPE.getId(entity.getType()).toString();
    }

    public boolean hasCustomName() {
        return entity.hasCustomName();
    }

    public double distanceTo(EntityView other) {
        return Math.sqrt(entity.squaredDistanceTo(other.entity));
    }

    public float getWidth() {
        return entity.getWidth();
    }

    public float getHeight() {
        return entity.getHeight();
    }
}
