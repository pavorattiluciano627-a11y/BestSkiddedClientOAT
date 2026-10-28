package dev.echo.client.scripting.api;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

public class EntityView {
    protected final Entity entity;

    public EntityView(Entity entity) {
        this.entity = entity;
    }

    public Vector3View getPosition() {
        return new Vector3View(entity.position());
    }

    public Vector3View getEyePosition() {
        return new Vector3View(entity.getEyePosition());
    }

    public float getYaw() {
        return entity.getYRot();
    }

    public float getPitch() {
        return entity.getXRot();
    }

    public boolean isAlive() {
        return entity.isAlive();
    }

    public int getId() {
        return entity.getId();
    }

    public int getAge() {
        return entity.tickCount;
    }

    public String getName() {
        return entity.getName().getString();
    }

    public String getDisplayName() {
        return entity.getDisplayName().getString();
    }

    public String getTypeId() {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    public boolean hasCustomName() {
        return entity.hasCustomName();
    }

    public double distanceTo(EntityView other) {
        return Math.sqrt(entity.distanceToSqr(other.entity));
    }

    public float getWidth() {
        return entity.getBbWidth();
    }

    public float getHeight() {
        return entity.getBbHeight();
    }
}
