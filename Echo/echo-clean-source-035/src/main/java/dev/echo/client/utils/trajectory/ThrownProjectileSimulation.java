package dev.echo.client.utils.trajectory;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Candidate for the gravity-before-drag thrown-projectile integration. */
public class ThrownProjectileSimulation extends ProjectileSimulation {
    private static final double GRAVITY = 0.05;
    private static final float AIR_DRAG = 0.99F;
    private static final float FLUID_DRAG = 0.8F;

    public ThrownProjectileSimulation(Vec3 position, Vec3 velocity) {
        this(position, velocity, Minecraft.getInstance().player);
    }

    public ThrownProjectileSimulation(Vec3 position, Vec3 velocity, Entity source) {
        super(position, velocity, source);
    }

    @Override
    public void step() {
        if (stopped || Minecraft.getInstance().level == null) {
            return;
        }
        boolean inFluid = !Minecraft.getInstance().level.getFluidState(BlockPos.containing(position)).isEmpty();
        velocity = velocity.subtract(0.0, GRAVITY, 0.0);
        velocity = velocity.scale(inFluid ? (double) FLUID_DRAG : (double) AIR_DRAG);
        Vec3 end = position.add(velocity);
        updateLeftOwner(end);
        BlockHitResult blockHit = raycastBlocks(position, end, collisionSource());
        EntityHitResult entityHit = raycastEntities(end);
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

    public void advance(int maxSteps) {
        for (int tick = 0; tick < maxSteps && !stopped; tick++) {
            step();
        }
    }

    public BlockHitResult findBlockHit(int maxSteps) {
        return findStoppingBlock(maxSteps);
    }
}
