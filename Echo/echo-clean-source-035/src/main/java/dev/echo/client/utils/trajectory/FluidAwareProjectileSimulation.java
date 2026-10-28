package dev.echo.client.utils.trajectory;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Complete conditional simulation body, with submerged-water drag and age-based collision margins. */
public class FluidAwareProjectileSimulation extends ProjectileSimulation {
    private static final double GRAVITY = 0.03;
    private static final float AIR_DRAG = 0.99F;
    private static final float WATER_DRAG = 0.8F;
    private int simulatedTicks;

    public FluidAwareProjectileSimulation(Vec3 position, Vec3 velocity) {
        this(position, velocity, Minecraft.getInstance().player);
    }

    public FluidAwareProjectileSimulation(Vec3 position, Vec3 velocity, Entity source) {
        super(position, velocity, source);
    }

    @Override
    public void step() {
        if (stopped || Minecraft.getInstance().level == null) {
            return;
        }
        velocity = velocity.subtract(0.0, GRAVITY, 0.0);
        velocity = velocity.scale(isSubmergedInWater() ? (double) WATER_DRAG : (double) AIR_DRAG);
        Vec3 end = position.add(velocity);
        updateLeftOwner(end);
        BlockHitResult blockHit = raycastBlocks(position, end, collisionSource());
        float margin = Math.max(0.0F, Math.min(0.3F, (float) (simulatedTicks - 2) / 20.0F));
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

    private boolean isSubmergedInWater() {
        BlockPos block = BlockPos.containing(position);
        var fluid = Minecraft.getInstance().level.getFluidState(block);
        // Addition occurs in float before widening; a full-block/any-fluid test is different.
        return fluid.is(FluidTags.WATER)
                && position.y - (double) ((float) block.getY()
                        + fluid.getHeight(Minecraft.getInstance().level, block)) < 0.0;
    }
}
