package dev.echo.client.utils.trajectory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Crossbow arrows retain movement-before-drag, friendly-fire filtering and axis rotation. */
public class CrossbowArrowSimulation extends ProjectileSimulation {
    private static final double LAUNCH_SPEED = 3.15;
    private static final double GRAVITY = 0.05;
    private static final float AIR_DRAG = 0.99F;
    private static final float FLUID_DRAG = 0.6F;

    public CrossbowArrowSimulation(PlayerEntity player, float angleDegrees, float tickDelta) {
        super(null, null, player);
        Vec3d direction = Vec3d.fromPolar(player.getPitch(tickDelta), player.getYaw(tickDelta)).normalize();
        Vec3d axis = player.getOppositeRotationVector(tickDelta).normalize();
        Vec3d rotated = rotateAroundAxis(direction, axis, angleDegrees);
        double x = MathHelper.lerp((double) tickDelta, player.lastX, player.getX());
        double y = MathHelper.lerp((double) tickDelta, player.lastY, player.getY()) + (double) player.getStandingEyeHeight();
        double z = MathHelper.lerp((double) tickDelta, player.lastZ, player.getZ());
        position = new Vec3d(x, y, z);
        velocity = rotated.multiply(LAUNCH_SPEED);
        Vec3d playerVelocity = player.getVelocity();
        velocity = velocity.add(playerVelocity.x, player.isOnGround() ? 0.0 : playerVelocity.y, playerVelocity.z);
        leftOwner = false;
    }

    @Override
    public void step() {
        if (stopped || MinecraftClient.getInstance().world == null) {
            return;
        }
        boolean inFluid = !MinecraftClient.getInstance().world.getFluidState(BlockPos.ofFloored(position)).isEmpty();
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
        velocity = velocity.multiply(inFluid ? (double) FLUID_DRAG : (double) AIR_DRAG);
        velocity = velocity.subtract(0.0, GRAVITY, 0.0);
    }

    @Override
    protected boolean canHit(Entity candidate) {
        return candidate.canBeHitByProjectile() && canDamagePlayer(candidate) && canHitOwner(candidate);
    }

    private static Vec3d rotateAroundAxis(Vec3d vector, Vec3d axis, float angleDegrees) {
        if (MathHelper.approximatelyEquals(angleDegrees, 0.0F)) {
            return vector;
        }
        Vec3d normalizedAxis = axis.normalize();
        double radians = angleDegrees * (float) (Math.PI / 180.0);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return vector.multiply(cosine)
                .add(normalizedAxis.crossProduct(vector).multiply(sine))
                .add(normalizedAxis.multiply(normalizedAxis.dotProduct(vector) * (1.0 - cosine)));
    }
}
