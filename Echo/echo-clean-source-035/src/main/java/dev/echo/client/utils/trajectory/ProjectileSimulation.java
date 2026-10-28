package dev.echo.client.utils.trajectory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Common candidate source for the repeated collision/owner operations. */
public abstract class ProjectileSimulation {
    protected static final double SEARCH_EXPANSION = 1.0;
    protected Vec3d position;
    protected Vec3d velocity;
    protected final Entity source;
    protected final Entity owner;
    protected boolean leftOwner;
    protected boolean stopped;

    protected ProjectileSimulation(Vec3d position, Vec3d velocity, Entity source) {
        this.position = position;
        this.velocity = velocity;
        this.source = source;
        this.owner = resolveOwner(source);
        this.leftOwner = owner == null;
        this.stopped = false;
    }

    protected static Entity resolveOwner(Entity source) {
        return source instanceof ProjectileEntity projectile ? projectile.getOwner() : source;
    }

    public final Vec3d getPosition() {
        return position;
    }

    public final Vec3d getVelocity() {
        return velocity;
    }

    public final boolean isStopped() {
        return stopped;
    }

    public abstract void step();

    protected final Entity collisionSource() {
        return source != null ? source : MinecraftClient.getInstance().player;
    }

    protected final BlockHitResult raycastBlocks(Vec3d start, Vec3d end, Entity context) {
        return MinecraftClient.getInstance().world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, context));
    }

    protected final EntityHitResult raycastEntities(Vec3d end) {
        Box searchBounds = new Box(position, end).expand(SEARCH_EXPANSION);
        return ProjectileUtil.raycast(collisionSource(), position, end,
                searchBounds, this::canHit,
                position.squaredDistanceTo(end));
    }

    protected final EntityHitResult collideEntities(Vec3d end, float margin) {
        Box searchBounds = new Box(position, end).expand(SEARCH_EXPANSION);
        return ProjectileUtil.getEntityCollision(MinecraftClient.getInstance().world,
                collisionSource(), position, end, searchBounds,
                this::canHit, margin);
    }

    protected boolean canHit(Entity candidate) {
        return candidate.canBeHitByProjectile() && canHitOwner(candidate);
    }

    protected final boolean canHitOwner(Entity candidate) {
        return owner == null || leftOwner || !owner.isConnectedThroughVehicle(candidate);
    }

    protected final boolean canDamagePlayer(Entity candidate) {
        return !(candidate instanceof PlayerEntity target && owner instanceof PlayerEntity player)
                || player.shouldDamagePlayer(target);
    }

    protected final void updateLeftOwner(Vec3d end) {
        if (leftOwner || owner == null) {
            return;
        }
        Box swept = new Box(position, end).expand(SEARCH_EXPANSION);
        leftOwner = owner.getRootVehicle().streamSelfAndPassengers()
                .noneMatch(entity -> intersectsOwner(swept, entity));
    }

    private static boolean intersectsOwner(Box swept, Entity entity) {
        return swept.intersects(entity.getBoundingBox());
    }

    protected final boolean entityHitComesFirst(BlockHitResult blockHit, EntityHitResult entityHit) {
        return entityHit != null && (blockHit.getType() == HitResult.Type.MISS
                || position.squaredDistanceTo(entityHit.getPos()) <= position.squaredDistanceTo(blockHit.getPos()));
    }

    protected final void stopAt(Vec3d hitPosition) {
        position = hitPosition;
        stopped = true;
    }

    protected final BlockHitResult findStoppingBlock(int maxSteps) {
        for (int tick = 0; tick < maxSteps && !stopped; tick++) {
            Vec3d start = position;
            step();
            if (stopped) {
                // The artifact reruns the block raycast even when an entity stopped the projectile.
                return raycastBlocks(start, position, MinecraftClient.getInstance().player);
            }
        }
        return null;
    }
}
