package dev.echo.client.utils.trajectory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Candidate for the gravity-before-drag thrown-projectile integration. */
public class ThrownProjectileSimulation extends ProjectileSimulation {
    private static final double GRAVITY = 0.05;
    private static final float AIR_DRAG = 0.99F;
    private static final float FLUID_DRAG = 0.8F;

    public ThrownProjectileSimulation(Vec3d position, Vec3d velocity) {
        this(position, velocity, MinecraftClient.getInstance().player);
    }

    public ThrownProjectileSimulation(Vec3d position, Vec3d velocity, Entity source) {
        super(position, velocity, source);
    }

    @Override
    public void step() {
        if (stopped || MinecraftClient.getInstance().world == null) {
            return;
        }
        boolean inFluid = !MinecraftClient.getInstance().world.getFluidState(BlockPos.ofFloored(position)).isEmpty();
        velocity = velocity.subtract(0.0, GRAVITY, 0.0);
        velocity = velocity.multiply(inFluid ? (double) FLUID_DRAG : (double) AIR_DRAG);
        Vec3d end = position.add(velocity);
        updateLeftOwner(end);
        BlockHitResult blockHit = raycastBlocks(position, end, collisionSource());
        EntityHitResult entityHit = raycastEntities(end);
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

    public void advance(int maxSteps) {
        for (int tick = 0; tick < maxSteps && !stopped; tick++) {
            step();
        }
    }

    public BlockHitResult findBlockHit(int maxSteps) {
        return findStoppingBlock(maxSteps);
    }
}
