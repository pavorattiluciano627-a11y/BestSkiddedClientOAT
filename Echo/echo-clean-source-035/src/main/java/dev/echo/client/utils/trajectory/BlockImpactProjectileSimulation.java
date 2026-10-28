package dev.echo.client.utils.trajectory;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Candidate trajectory that retains the exact block impact and expands entity hits with age. */
public class BlockImpactProjectileSimulation extends ProjectileSimulation {
    private static final double GRAVITY = 0.05;
    private static final float AIR_DRAG = 0.99F;
    private static final float FLUID_DRAG = 0.6F;
    private int age;
    private BlockHitResult blockImpact;

    public BlockImpactProjectileSimulation(Vec3 position, Vec3 velocity) {
        this(position, velocity, null);
    }

    public BlockImpactProjectileSimulation(Vec3 position, Vec3 velocity, Entity source) {
        super(position, velocity, source);
        age = source == null ? 0 : source.tickCount;
    }

    @Override
    public void step() {
        if (stopped || Minecraft.getInstance().level == null) {
            return;
        }
        age++;
        boolean inFluid = !Minecraft.getInstance().level.getFluidState(BlockPos.containing(position)).isEmpty();
        Vec3 end = position.add(velocity);
        updateLeftOwner(end);
        BlockHitResult blockHit = raycastBlocks(position, end, collisionSource());
        EntityHitResult entityHit = collideEntities(end, Math.max(0.0F, Math.min(0.3F, (age - 2) / 20.0F)));
        if (entityHitComesFirst(blockHit, entityHit)) {
            stopAt(entityHit.getLocation());
            return;
        }
        if (blockHit.getType() != HitResult.Type.MISS) {
            position = blockHit.getLocation();
            blockImpact = blockHit;
            stopped = true;
            return;
        }
        position = end;
        velocity = velocity.scale(inFluid ? (double) FLUID_DRAG : (double) AIR_DRAG);
        velocity = velocity.subtract(0.0, GRAVITY, 0.0);
    }

    @Override
    protected boolean canHit(Entity candidate) {
        return candidate.canBeHitByProjectile() && canDamagePlayer(candidate) && canHitOwner(candidate);
    }

    public void advance(int maxSteps) {
        for (int tick = 0; tick < maxSteps && !stopped; tick++) {
            step();
        }
    }

    public BlockHitResult findBlockHit(int maxSteps) {
        advance(maxSteps);
        return blockImpact;
    }

    public BlockHitResult getBlockImpact() {
        return blockImpact;
    }
}
