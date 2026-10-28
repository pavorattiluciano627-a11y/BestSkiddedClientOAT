package dev.echo.client.utils.trajectory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Candidate bow simulation. Movement precedes drag and gravity in this variant. */
public class BowSimulation extends ProjectileSimulation {
    private static final double GRAVITY = 0.05;
    private static final float AIR_DRAG = 0.99F;
    private static final float FLUID_DRAG = 0.6F;

    public BowSimulation(PlayerEntity player, int useTicks, float tickDelta) {
        super(new Vec3d(
                        MathHelper.lerp((double) tickDelta, player.lastX, player.getX()),
                        MathHelper.lerp((double) tickDelta, player.lastY, player.getY()) + player.getStandingEyeHeight(),
                        MathHelper.lerp((double) tickDelta, player.lastZ, player.getZ())),
                launchVelocity(player.getPitch(tickDelta), player.getYaw(tickDelta), BowItem.getPullProgress(useTicks) * 3.0F), player);
        Vec3d playerVelocity = player.getVelocity();
        velocity = velocity.add(playerVelocity.x, player.isOnGround() ? 0.0 : playerVelocity.y, playerVelocity.z);
    }

    public BowSimulation(Vec3d position, float pitch, float yaw, float pullProgress) {
        super(position, launchVelocity(pitch, yaw, pullProgress * 3.0F), MinecraftClient.getInstance().player);
    }

    static Vec3d launchVelocity(float pitch, float yaw, float speed) {
        float radians = (float) Math.PI / 180.0F;
        float x = -MathHelper.sin((double) (yaw * radians)) * MathHelper.cos((double) (pitch * radians));
        float y = -MathHelper.sin((double) (pitch * radians));
        float z = MathHelper.cos((double) (yaw * radians)) * MathHelper.cos((double) (pitch * radians));
        return new Vec3d(x, y, z).normalize().multiply((double) speed);
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

    public BlockHitResult findBlockHit(int maxSteps) {
        return findStoppingBlock(maxSteps);
    }
}
