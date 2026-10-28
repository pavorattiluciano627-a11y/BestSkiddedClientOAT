package dev.echo.client.handlers;

import dev.echo.client.events.EventBus;
import dev.echo.client.events.world.EntityHitboxEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.utils.Rotation;
import dev.echo.client.utils.targeting.AimPointMode;
import dev.echo.client.utils.targeting.AimPointSettings;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Owned rotation request, transition state, and cached visible target point.
 * Conditional source candidate; original member/type-cache and key-domain
 * applicability remains an explicit integration obligation.
 */
public final class RotationRequest {
    private final RotationManager manager;
    private final Feature owner;
    private final RotationPriority priority;
    private RotationSettings settings;
    private Rotation desiredRotation;
    private LivingEntity target;
    private int refreshedTick;
    private boolean active = true;
    private boolean returning;
    private long previousUpdateNanos;
    private float yawVelocity;
    private float pitchVelocity;
    private LivingEntity cachedTarget;
    private Vec3 cachedLocalPoint;

    RotationRequest(RotationManager manager, Feature owner, RotationPriority priority) {
        this.manager = manager;
        this.owner = owner;
        this.priority = priority;
    }

    /** Sets a world-space aim point for this request. */
    public boolean aim(Vec3 point, RotationSettings settings) {
        return manager.aimAt(this, point, settings);
    }

    /** Selects an entity hitbox point and updates this request. */
    public boolean aim(LivingEntity target, AimPointSettings targeting, RotationSettings settings) {
        return manager.aimAt(this, target, targeting, settings);
    }

    /** Extends the request's ownership through the current manager update. */
    public void refresh() {
        manager.refresh(this);
    }

    /** Releases a visible request or begins returning a silent request to the player. */
    public void release() {
        manager.release(this);
    }

    /** Returns whether the request is live and still owned by its manager. */
    public boolean isActive() {
        return active && manager.isCurrent(this);
    }

    Feature owner() { return owner; }
    RotationPriority priority() { return priority; }
    RotationSettings settings() { return settings; }
    Rotation desiredRotation() { return desiredRotation; }
    LivingEntity target() { return target; }
    int refreshedTick() { return refreshedTick; }
    boolean returning() { return returning; }
    void settings(RotationSettings value) { settings = value; }
    void target(Rotation rotation, LivingEntity entity) { desiredRotation = rotation; target = entity; }
    void refreshedTick(int tick) { refreshedTick = tick; }
    void returning(boolean value) { returning = value; }

    Rotation transition(Rotation current, Rotation desired, RotationSettings settings, float elapsedSeconds) {
        return switch (settings.transition()) {
            case SNAP -> desired;
            case EASED -> eased(current, desired, settings.maxYawStep(), settings.maxPitchStep(), elapsedSeconds);
            case NATURAL -> natural(current, desired, settings.maxYawStep(), settings.maxPitchStep(), elapsedSeconds);
        };
    }

    float elapsedSeconds(long now) {
        if (previousUpdateNanos == 0L) {
            previousUpdateNanos = now;
            return 0.016666668F;
        }
        float elapsed = Math.clamp((float) (now - previousUpdateNanos) / 1.0E9F, 0.001F, 0.1F);
        previousUpdateNanos = now;
        return elapsed;
    }

    void resetClock() { previousUpdateNanos = 0L; }

    void invalidate() {
        active = false;
        returning = false;
        previousUpdateNanos = 0L;
        yawVelocity = 0.0F;
        pitchVelocity = 0.0F;
        cachedTarget = null;
        cachedLocalPoint = null;
    }

    Optional<Vec3> targetPoint(LivingEntity entity, AimPointSettings targeting, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (entity == null || client.player == null || client.level == null) return Optional.empty();
        AABB box = interpolatedHitbox(entity, tickDelta);
        Vec3 center = box.getCenter();
        Vec3 selected = switch (targeting.mode()) {
            case STRAIGHT -> new Vec3(center.x, box.maxY - 0.1, center.z);
            case CLOSEST -> {
                Vec3 eyes = client.player.getEyePosition(tickDelta);
                yield new Vec3(Mth.clamp(eyes.x, box.minX, box.maxX),
                        Mth.clamp(eyes.y, box.minY, box.maxY), Mth.clamp(eyes.z, box.minZ, box.maxZ));
            }
            case RANDOM -> RotationMath.randomPoint(box);
            case CENTER -> center;
            case FEET -> new Vec3(center.x, box.minY + 0.1, center.z);
        };
        if (targeting.randomize()) selected = randomizePoint(selected, box, targeting.mode());
        Vec3 preferred = center.lerp(selected, (double) targeting.pointWeight() / 100.0);
        if (visible(preferred, tickDelta)) {
            cachedTarget = null;
            cachedLocalPoint = null;
            return Optional.of(preferred);
        }
        if (cachedTarget == entity && cachedLocalPoint != null) {
            Vec3 previous = cachedLocalPoint.add(box.minX, box.minY, box.minZ);
            if (containsInclusive(previous, box) && visible(previous, tickDelta)) return Optional.of(previous);
        }
        Vec3 alternative = closestVisibleGridPoint(box, preferred, tickDelta);
        if (alternative != null) {
            cachedTarget = entity;
            cachedLocalPoint = alternative.subtract(box.minX, box.minY, box.minZ);
            return Optional.of(alternative);
        }
        cachedTarget = null;
        cachedLocalPoint = null;
        return targeting.allowOccluded() ? Optional.of(preferred) : Optional.empty();
    }

    private static AABB interpolatedHitbox(LivingEntity entity, float tickDelta) {
        EntityHitboxEvent event = new EntityHitboxEvent(entity, entity.getBoundingBox().inflate(entity.getPickRadius()));
        EventBus.post(event);
        Vec3 interpolated = entity.getPosition(tickDelta);
        return event.getBoundingBox().move(interpolated.x - entity.getX(),
                interpolated.y - entity.getY(), interpolated.z - entity.getZ());
    }

    private static Rotation eased(Rotation current, Rotation desired, float yawSpeed, float pitchSpeed, float elapsed) {
        float yaw = Mth.wrapDegrees(desired.yaw() - current.yaw());
        float pitch = desired.pitch() - current.pitch();
        float yawStep = Math.min(Math.abs(yaw), Math.max(yawSpeed * elapsed * 0.2F,
                Math.abs(yaw) * (1.0F - (float) Math.exp(-yawSpeed * elapsed * 0.1F))));
        float pitchStep = Math.min(Math.abs(pitch), Math.max(pitchSpeed * elapsed * 0.2F,
                Math.abs(pitch) * (1.0F - (float) Math.exp(-pitchSpeed * elapsed * 0.1F))));
        return new Rotation(current.yaw() + Math.signum(yaw) * yawStep,
                current.pitch() + Math.signum(pitch) * pitchStep);
    }

    private Rotation natural(Rotation current, Rotation desired, float yawSpeed, float pitchSpeed, float elapsed) {
        elapsed = Mth.clamp(elapsed, 0.001F, 0.1F);
        float yaw = Mth.wrapDegrees(desired.yaw() - current.yaw());
        float pitch = desired.pitch() - current.pitch();
        yawVelocity = RotationMath.smoothVelocity(yawVelocity, yaw, yawSpeed, elapsed);
        pitchVelocity = RotationMath.smoothVelocity(pitchVelocity, pitch, pitchSpeed, elapsed);
        return new Rotation(current.yaw() + yawVelocity * elapsed, current.pitch() + pitchVelocity * elapsed);
    }

    private static Vec3 randomizePoint(Vec3 point, AABB box, AimPointMode mode) {
        if (mode != AimPointMode.CLOSEST) return point.lerp(RotationMath.randomPoint(box), 0.5);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 offset = new Vec3(point.x + (random.nextDouble() - 0.5) * (box.maxX - box.minX) * 0.9,
                point.y + (random.nextDouble() - 0.5) * (box.maxY - box.minY) * 0.9,
                point.z + (random.nextDouble() - 0.5) * (box.maxZ - box.minZ) * 0.9);
        return point.lerp(new Vec3(Mth.clamp(offset.x, box.minX, box.maxX),
                Mth.clamp(offset.y, box.minY, box.maxY), Mth.clamp(offset.z, box.minZ, box.maxZ)), 0.5);
    }

    private static Vec3 closestVisibleGridPoint(AABB box, Vec3 preferred, float tickDelta) {
        Vec3 closest = null;
        double distance = Double.MAX_VALUE;
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 3; z++) {
                    Vec3 point = new Vec3(box.minX + (double) x / 2.0 * (box.maxX - box.minX),
                            box.minY + (double) y / 4.0 * (box.maxY - box.minY),
                            box.minZ + (double) z / 2.0 * (box.maxZ - box.minZ));
                    if (visible(point, tickDelta) && point.distanceToSqr(preferred) < distance) {
                        distance = point.distanceToSqr(preferred);
                        closest = point;
                    }
                }
            }
        }
        return closest;
    }

    private static boolean visible(Vec3 point, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return true;
        HitResult hit = client.level.clip(new ClipContext(client.player.getEyePosition(tickDelta), point,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, client.player));
        return hit == null || hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(point) < 1.0E-6;
    }

    private static boolean containsInclusive(Vec3 point, AABB box) {
        return point.x >= box.minX && point.x <= box.maxX && point.y >= box.minY
                && point.y <= box.maxY && point.z >= box.minZ && point.z <= box.maxZ;
    }
}
