package dev.echo.client.utils.trajectory;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Candidate bow simulation. Movement precedes drag and gravity in this variant. */
public class BowSimulation extends ProjectileSimulation {
    private static final double GRAVITY = 0.05;
    private static final float AIR_DRAG = 0.99F;
    private static final float FLUID_DRAG = 0.6F;

    public BowSimulation(Player player, int useTicks, float tickDelta) {
        super(new Vec3(
                        Mth.lerp((double) tickDelta, player.xo, player.getX()),
                        Mth.lerp((double) tickDelta, player.yo, player.getY()) + player.getEyeHeight(),
                        Mth.lerp((double) tickDelta, player.zo, player.getZ())),
                launchVelocity(player.getViewXRot(tickDelta), player.getViewYRot(tickDelta), BowItem.getPowerForTime(useTicks) * 3.0F), player);
        Vec3 playerVelocity = player.getDeltaMovement();
        velocity = velocity.add(playerVelocity.x, player.onGround() ? 0.0 : playerVelocity.y, playerVelocity.z);
    }

    public BowSimulation(Vec3 position, float pitch, float yaw, float pullProgress) {
        super(position, launchVelocity(pitch, yaw, pullProgress * 3.0F), Minecraft.getInstance().player);
    }

    static Vec3 launchVelocity(float pitch, float yaw, float speed) {
        float radians = (float) Math.PI / 180.0F;
        float x = -Mth.sin((double) (yaw * radians)) * Mth.cos((double) (pitch * radians));
        float y = -Mth.sin((double) (pitch * radians));
        float z = Mth.cos((double) (yaw * radians)) * Mth.cos((double) (pitch * radians));
        return new Vec3(x, y, z).normalize().scale((double) speed);
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

    public BlockHitResult findBlockHit(int maxSteps) {
        return findStoppingBlock(maxSteps);
    }
}
