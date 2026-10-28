package dev.echo.client.utils.trajectory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

/** Candidate constant-velocity wind-charge trajectory. */
public class WindChargeSimulation extends ProjectileSimulation {
    private int simulatedTicks;

    public WindChargeSimulation(Vec3d position, Vec3d velocity) {
        this(position, velocity, MinecraftClient.getInstance().player);
    }

    public WindChargeSimulation(Vec3d position, Vec3d velocity, Entity source) {
        super(position, velocity, source);
    }

    @Override
    public void step() {
        if (stopped || MinecraftClient.getInstance().world == null) {
            return;
        }
        Vec3d end = position.add(velocity);
        updateLeftOwner(end);
        BlockHitResult blockHit = raycastBlocks(position, end, collisionSource());
        float margin = Math.max(0.0F, Math.min(0.3F, (simulatedTicks - 2) / 20.0F));
        EntityHitResult entityHit = collideEntities(end, margin);
        simulatedTicks++;
        if (entityHitComesFirst(blockHit, entityHit)) {
            stopAt(entityHit.getPos());
            return;
        }
        if (blockHit.getType() != HitResult.Type.MISS) {
            stopAt(blockHit.getPos());
            return;
        }
        position = end;
    }

    @Override
    protected boolean canHit(Entity candidate) {
        return candidate.canBeHitByProjectile() && !candidate.noClip
                && !(candidate instanceof AbstractWindChargeEntity)
                && candidate.getType() != EntityType.END_CRYSTAL
                && canHitOwner(candidate);
    }
}
