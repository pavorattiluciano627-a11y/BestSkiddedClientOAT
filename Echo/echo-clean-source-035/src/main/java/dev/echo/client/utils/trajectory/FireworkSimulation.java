package dev.echo.client.utils.trajectory;

import dev.echo.client.mixin.FireworkRocketEntityAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Complete conditional firework simulation, including both original constructors.
 * The two original decoder slots yield the same lifetime salt; see lifetime-salts.json.
 */
public class FireworkSimulation {
    private static final float LAUNCH_SPEED = 1.6F;
    private static final double SEARCH_EXPANSION = 1.0;
    private static final long LIFETIME_SEED_SALT = 7640891576956012809L;
    private Vec3d position;
    private Vec3d velocity;
    private final Entity source;
    private final Entity owner;
    private final boolean shotAtAngle;
    private final int lifetime;
    private int life;
    private boolean leftOwner;
    private boolean stopped;

    public FireworkSimulation(PlayerEntity player, float angleDegrees, ItemStack stack,
                              int shotIndex, float tickDelta) {
        Vec3d direction = Vec3d.fromPolar(player.getPitch(tickDelta), player.getYaw(tickDelta)).normalize();
        Vec3d axis = player.getOppositeRotationVector(tickDelta).normalize();
        Vec3d rotated = FireworkTrajectoryMath.rotateAroundAxis(direction, axis, angleDegrees);
        double x = MathHelper.lerp((double) tickDelta, player.lastX, player.getX());
        double y = MathHelper.lerp((double) tickDelta, player.lastY, player.getY())
                + (double) player.getStandingEyeHeight() - (double) 0.15F;
        double z = MathHelper.lerp((double) tickDelta, player.lastZ, player.getZ());
        position = new Vec3d(x, y, z);
        velocity = FireworkTrajectoryMath.applyDivergence(rotated, FireworkSeeds.forShot(player, stack, shotIndex))
                .multiply((double) LAUNCH_SPEED);
        Vec3d playerVelocity = player.getVelocity();
        velocity = velocity.add(playerVelocity.x, player.isOnGround() ? 0.0 : playerVelocity.y, playerVelocity.z);
        source = player;
        owner = player;
        shotAtAngle = true;
        leftOwner = false;
        life = 0;
        // This is a second seed computation, as in the original constructor.
        lifetime = FireworkTrajectoryMath.lifetime(stack, FireworkSeeds.forShot(player, stack, shotIndex) ^ LIFETIME_SEED_SALT);
        stopped = false;
    }

    public FireworkSimulation(FireworkRocketEntity rocket) {
        position = rocket.getEntityPos();
        velocity = rocket.getVelocity();
        source = rocket;
        owner = ((ProjectileEntity) rocket).getOwner();
        shotAtAngle = rocket.wasShotAtAngle();
        // Deliberate casts: an unmixed rocket fails here; no reflective/default fallback.
        int trackedLife = ((FireworkRocketEntityAccessor) rocket).getLife();
        int trackedLifetime = ((FireworkRocketEntityAccessor) rocket).getLifeTime();
        boolean lifetimeKnown = trackedLifetime > 0;
        life = Math.max(0, lifetimeKnown ? trackedLife : rocket.age);
        lifetime = lifetimeKnown ? trackedLifetime : FireworkTrajectoryMath.lifetime(
                rocket.getStack(), FireworkSeeds.forRocket(rocket) ^ LIFETIME_SEED_SALT);
        leftOwner = owner == null;
        stopped = false;
    }

    public void step() {
        if (stopped || MinecraftClient.getInstance().world == null) {
            return;
        }
        if (!shotAtAngle) {
            velocity = velocity.multiply(1.15, 1.0, 1.15).add(0.0, 0.04, 0.0);
        }
        Vec3d end = position.add(velocity);
        updateLeftOwner(end);
        BlockHitResult blockHit = MinecraftClient.getInstance().world.raycast(new RaycastContext(
                position, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE,
                source != null ? source : MinecraftClient.getInstance().player));
        EntityHitResult entityHit = raycastEntities(end);
        if (entityHit != null && (blockHit.getType() == HitResult.Type.MISS
                || position.squaredDistanceTo(entityHit.getPos()) <= position.squaredDistanceTo(blockHit.getPos()))) {
            position = entityHit.getPos();
            stopped = true;
            return;
        }
        if (blockHit.getType() != HitResult.Type.MISS) {
            position = blockHit.getPos();
            stopped = true;
            return;
        }
        position = end;
        life++;
        if (life > lifetime) {
            stopped = true;
        }
    }

    public Vec3d getPosition() {
        return position;
    }

    public boolean isStopped() {
        return stopped;
    }

    private EntityHitResult raycastEntities(Vec3d end) {
        Box swept = new Box(position, end).expand(SEARCH_EXPANSION);
        return ProjectileUtil.raycast(source != null ? source : MinecraftClient.getInstance().player,
                position, end, swept, this::canHit, position.squaredDistanceTo(end));
    }

    private boolean canHit(Entity candidate) {
        return candidate.canBeHitByProjectile()
                && (owner == null || leftOwner || !owner.isConnectedThroughVehicle(candidate));
    }

    private void updateLeftOwner(Vec3d end) {
        if (leftOwner || owner == null) {
            return;
        }
        Box swept = new Box(position, end).expand(SEARCH_EXPANSION);
        leftOwner = owner.getRootVehicle().streamSelfAndPassengers()
                .noneMatch(entity -> swept.intersects(entity.getBoundingBox()));
    }
}
