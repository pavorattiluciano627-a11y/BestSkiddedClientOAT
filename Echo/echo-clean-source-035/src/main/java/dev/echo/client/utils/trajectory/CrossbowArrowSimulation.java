package dev.echo.client.utils.trajectory;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Crossbow arrows retain movement-before-drag, friendly-fire filtering and axis rotation. */
public class CrossbowArrowSimulation extends ProjectileSimulation {
    private static final double LAUNCH_SPEED = 3.15;
    private static final double GRAVITY = 0.05;
    private static final float AIR_DRAG = 0.99F;
    private static final float FLUID_DRAG = 0.6F;

    public CrossbowArrowSimulation(Player player, float angleDegrees, float tickDelta) {
        super(null, null, player);
        Vec3 direction = Vec3.directionFromRotation(player.getViewXRot(tickDelta), player.getViewYRot(tickDelta)).normalize();
        Vec3 axis = player.getUpVector(tickDelta).normalize();
        Vec3 rotated = rotateAroundAxis(direction, axis, angleDegrees);
        double x = Mth.lerp((double) tickDelta, player.xo, player.getX());
        double y = Mth.lerp((double) tickDelta, player.yo, player.getY()) + (double) player.getEyeHeight();
        double z = Mth.lerp((double) tickDelta, player.zo, player.getZ());
        position = new Vec3(x, y, z);
        velocity = rotated.scale(LAUNCH_SPEED);
        Vec3 playerVelocity = player.getDeltaMovement();
        velocity = velocity.add(playerVelocity.x, player.onGround() ? 0.0 : playerVelocity.y, playerVelocity.z);
        leftOwner = false;
    }

    @Override
    public void step() {
        if (stopped || Minecraft.getInstance().level == null) {
            return;
        }
        boolean inFluid = !Minecraft.getInstance().level.getFluidState(BlockPos.containing(position)).isEmpty();
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
        velocity = velocity.scale(inFluid ? (double) FLUID_DRAG : (double) AIR_DRAG);
        velocity = velocity.subtract(0.0, GRAVITY, 0.0);
    }

    @Override
    protected boolean canHit(Entity candidate) {
        return candidate.canBeHitByProjectile() && canDamagePlayer(candidate) && canHitOwner(candidate);
    }

    private static Vec3 rotateAroundAxis(Vec3 vector, Vec3 axis, float angleDegrees) {
        if (Mth.equal(angleDegrees, 0.0F)) {
            return vector;
        }
        Vec3 normalizedAxis = axis.normalize();
        double radians = angleDegrees * (float) (Math.PI / 180.0);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return vector.scale(cosine)
                .add(normalizedAxis.cross(vector).scale(sine))
                .add(normalizedAxis.scale(normalizedAxis.dot(vector) * (1.0 - cosine)));
    }
}
