package dev.echo.client.utils.trajectory;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Candidate constant-velocity wind-charge trajectory. */
public class WindChargeSimulation extends ProjectileSimulation {
    private int simulatedTicks;

    public WindChargeSimulation(Vec3 position, Vec3 velocity) {
        this(position, velocity, Minecraft.getInstance().player);
    }

    public WindChargeSimulation(Vec3 position, Vec3 velocity, Entity source) {
        super(position, velocity, source);
    }

    @Override
    public void step() {
        if (stopped || Minecraft.getInstance().level == null) {
            return;
        }
        Vec3 end = position.add(velocity);
        updateLeftOwner(end);
        BlockHitResult blockHit = raycastBlocks(position, end, collisionSource());
        float margin = Math.max(0.0F, Math.min(0.3F, (simulatedTicks - 2) / 20.0F));
        EntityHitResult entityHit = collideEntities(end, margin);
        simulatedTicks++;
        if (entityHitComesFirst(blockHit, entityHit)) {
            stopAt(entityHit.getLocation());
            return;
        }
        if (blockHit.getType() != HitResult.Type.MISS) {
            stopAt(blockHit.getLocation());
            return;
        }
        position = end;
    }

    @Override
    protected boolean canHit(Entity candidate) {
        return candidate.canBeHitByProjectile() && !candidate.noPhysics
                && !(candidate instanceof AbstractWindCharge)
                && candidate.getType() != EntityTypes.END_CRYSTAL
                && canHitOwner(candidate);
    }
}
