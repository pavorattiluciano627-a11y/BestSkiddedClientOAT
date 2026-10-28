package dev.echo.client.utils.trajectory;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Common candidate source for the repeated collision/owner operations. */
public abstract class ProjectileSimulation {
    protected static final double SEARCH_EXPANSION = 1.0;
    protected Vec3 position;
    protected Vec3 velocity;
    protected final Entity source;
    protected final Entity owner;
    protected boolean leftOwner;
    protected boolean stopped;

    protected ProjectileSimulation(Vec3 position, Vec3 velocity, Entity source) {
        this.position = position;
        this.velocity = velocity;
        this.source = source;
        this.owner = resolveOwner(source);
        this.leftOwner = owner == null;
        this.stopped = false;
    }

    protected static Entity resolveOwner(Entity source) {
        return source instanceof Projectile projectile ? projectile.getOwner() : source;
    }

    public final Vec3 getPosition() {
        return position;
    }

    public final Vec3 getVelocity() {
        return velocity;
    }

    public final boolean isStopped() {
        return stopped;
    }

    public abstract void step();

    protected final Entity collisionSource() {
        return source != null ? source : Minecraft.getInstance().player;
    }

    protected final BlockHitResult raycastBlocks(Vec3 start, Vec3 end, Entity context) {
        return Minecraft.getInstance().level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context));
    }

    protected final EntityHitResult raycastEntities(Vec3 end) {
        AABB searchBounds = new AABB(position, end).inflate(SEARCH_EXPANSION);
        return ProjectileUtil.getEntityHitResult(collisionSource(), position, end,
                searchBounds, this::canHit,
                position.distanceToSqr(end));
    }

    protected final EntityHitResult collideEntities(Vec3 end, float margin) {
        AABB searchBounds = new AABB(position, end).inflate(SEARCH_EXPANSION);
        return ProjectileUtil.getEntityHitResult(Minecraft.getInstance().level,
                collisionSource(), position, end, searchBounds,
                this::canHit, margin);
    }

    protected boolean canHit(Entity candidate) {
        return candidate.canBeHitByProjectile() && canHitOwner(candidate);
    }

    protected final boolean canHitOwner(Entity candidate) {
        return owner == null || leftOwner || !owner.isPassengerOfSameVehicle(candidate);
    }

    protected final boolean canDamagePlayer(Entity candidate) {
        return !(candidate instanceof Player target && owner instanceof Player player)
                || player.canHarmPlayer(target);
    }

    protected final void updateLeftOwner(Vec3 end) {
        if (leftOwner || owner == null) {
            return;
        }
        AABB swept = new AABB(position, end).inflate(SEARCH_EXPANSION);
        leftOwner = owner.getRootVehicle().getSelfAndPassengers()
                .noneMatch(entity -> intersectsOwner(swept, entity));
    }

    private static boolean intersectsOwner(AABB swept, Entity entity) {
        return swept.intersects(entity.getBoundingBox());
    }

    protected final boolean entityHitComesFirst(BlockHitResult blockHit, EntityHitResult entityHit) {
        return entityHit != null && (blockHit.getType() == HitResult.Type.MISS
                || position.distanceToSqr(entityHit.getLocation()) <= position.distanceToSqr(blockHit.getLocation()));
    }

    protected final void stopAt(Vec3 hitPosition) {
        position = hitPosition;
        stopped = true;
    }

    protected final BlockHitResult findStoppingBlock(int maxSteps) {
        for (int tick = 0; tick < maxSteps && !stopped; tick++) {
            Vec3 start = position;
            step();
            if (stopped) {
                // The artifact reruns the block raycast even when an entity stopped the projectile.
                return raycastBlocks(start, position, Minecraft.getInstance().player);
            }
        }
        return null;
    }
}
