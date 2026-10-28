package dev.echo.client.utils.trajectory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Complete conditional simulation body, with submerged-water drag and age-based collision margins. */
public class FluidAwareProjectileSimulation extends ProjectileSimulation {
    private static final double GRAVITY = 0.03;
    private static final float AIR_DRAG = 0.99F;
    private static final float WATER_DRAG = 0.8F;
    private int simulatedTicks;

    public FluidAwareProjectileSimulation(Vec3d position, Vec3d velocity) {
        this(position, velocity, MinecraftClient.getInstance().player);
    }

    public FluidAwareProjectileSimulation(Vec3d position, Vec3d velocity, Entity source) {
        super(position, velocity, source);
    }

    @Override
    public void step() {
        if (stopped || MinecraftClient.getInstance().world == null) {
            return;
        }
        velocity = velocity.subtract(0.0, GRAVITY, 0.0);
        velocity = velocity.multiply(isSubmergedInWater() ? (double) WATER_DRAG : (double) AIR_DRAG);
        Vec3d end = position.add(velocity);
        updateLeftOwner(end);
        BlockHitResult blockHit = raycastBlocks(position, end, collisionSource());
        float margin = Math.max(0.0F, Math.min(0.3F, (float) (simulatedTicks - 2) / 20.0F));
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

    private boolean isSubmergedInWater() {
        BlockPos block = BlockPos.ofFloored(position);
        var fluid = MinecraftClient.getInstance().world.getFluidState(block);
        // Addition occurs in float before widening; a full-block/any-fluid test is different.
        return fluid.isIn(FluidTags.WATER)
                && position.y - (double) ((float) block.getY()
                        + fluid.getHeight(MinecraftClient.getInstance().world, block)) < 0.0;
    }
}
