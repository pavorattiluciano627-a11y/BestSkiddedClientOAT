package dev.echo.client.utils.trajectory;

import dev.echo.client.mixin.FireworkRocketEntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Complete conditional firework simulation, including both original constructors.
 * The two original decoder slots yield the same lifetime salt; see lifetime-salts.json.
 */
public class FireworkSimulation {
    private static final float LAUNCH_SPEED = 1.6F;
    private static final double SEARCH_EXPANSION = 1.0;
    private static final long LIFETIME_SEED_SALT = 7640891576956012809L;
    private Vec3 position;
    private Vec3 velocity;
    private final Entity source;
    private final Entity owner;
    private final boolean shotAtAngle;
    private final int lifetime;
    private int life;
    private boolean leftOwner;
    private boolean stopped;

    public FireworkSimulation(Player player, float angleDegrees, ItemStack stack,
                              int shotIndex, float tickDelta) {
        Vec3 direction = Vec3.directionFromRotation(player.getViewXRot(tickDelta), player.getViewYRot(tickDelta)).normalize();
        Vec3 axis = player.getUpVector(tickDelta).normalize();
        Vec3 rotated = FireworkTrajectoryMath.rotateAroundAxis(direction, axis, angleDegrees);
        double x = Mth.lerp((double) tickDelta, player.xo, player.getX());
        double y = Mth.lerp((double) tickDelta, player.yo, player.getY())
                + (double) player.getEyeHeight() - (double) 0.15F;
        double z = Mth.lerp((double) tickDelta, player.zo, player.getZ());
        position = new Vec3(x, y, z);
        velocity = FireworkTrajectoryMath.applyDivergence(rotated, FireworkSeeds.forShot(player, stack, shotIndex))
                .scale((double) LAUNCH_SPEED);
        Vec3 playerVelocity = player.getDeltaMovement();
        velocity = velocity.add(playerVelocity.x, player.onGround() ? 0.0 : playerVelocity.y, playerVelocity.z);
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
        position = rocket.position();
        velocity = rocket.getDeltaMovement();
        source = rocket;
        owner = ((Projectile) rocket).getOwner();
        shotAtAngle = rocket.isShotAtAngle();
        // Deliberate casts: an unmixed rocket fails here; no reflective/default fallback.
        int trackedLife = ((FireworkRocketEntityAccessor) rocket).getLife();
        int trackedLifetime = ((FireworkRocketEntityAccessor) rocket).getLifeTime();
        boolean lifetimeKnown = trackedLifetime > 0;
        life = Math.max(0, lifetimeKnown ? trackedLife : rocket.tickCount);
        lifetime = lifetimeKnown ? trackedLifetime : FireworkTrajectoryMath.lifetime(
                rocket.getItem(), FireworkSeeds.forRocket(rocket) ^ LIFETIME_SEED_SALT);
        leftOwner = owner == null;
        stopped = false;
    }

    public void step() {
        if (stopped || Minecraft.getInstance().level == null) {
            return;
        }
        if (!shotAtAngle) {
            velocity = velocity.multiply(1.15, 1.0, 1.15).add(0.0, 0.04, 0.0);
        }
        Vec3 end = position.add(velocity);
        updateLeftOwner(end);
        BlockHitResult blockHit = Minecraft.getInstance().level.clip(new ClipContext(
                position, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                source != null ? source : Minecraft.getInstance().player));
        EntityHitResult entityHit = raycastEntities(end);
        if (entityHit != null && (blockHit.getType() == HitResult.Type.MISS
                || position.distanceToSqr(entityHit.getLocation()) <= position.distanceToSqr(blockHit.getLocation()))) {
            position = entityHit.getLocation();
            stopped = true;
            return;
        }
        if (blockHit.getType() != HitResult.Type.MISS) {
            position = blockHit.getLocation();
            stopped = true;
            return;
        }
        position = end;
        life++;
        if (life > lifetime) {
            stopped = true;
        }
    }

    public Vec3 getPosition() {
        return position;
    }

    public boolean isStopped() {
        return stopped;
    }

    private EntityHitResult raycastEntities(Vec3 end) {
        AABB swept = new AABB(position, end).inflate(SEARCH_EXPANSION);
        return ProjectileUtil.getEntityHitResult(source != null ? source : Minecraft.getInstance().player,
                position, end, swept, this::canHit, position.distanceToSqr(end));
    }

    private boolean canHit(Entity candidate) {
        return candidate.canBeHitByProjectile()
                && (owner == null || leftOwner || !owner.isPassengerOfSameVehicle(candidate));
    }

    private void updateLeftOwner(Vec3 end) {
        if (leftOwner || owner == null) {
            return;
        }
        AABB swept = new AABB(position, end).inflate(SEARCH_EXPANSION);
        leftOwner = owner.getRootVehicle().getSelfAndPassengers()
                .noneMatch(entity -> swept.intersects(entity.getBoundingBox()));
    }
}
