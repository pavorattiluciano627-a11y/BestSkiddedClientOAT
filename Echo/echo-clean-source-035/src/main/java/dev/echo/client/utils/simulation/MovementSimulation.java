package dev.echo.client.utils.simulation;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.world.EntityHitboxEvent;
import dev.echo.client.mixin.EntityAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Cached player fall prediction and target-hitbox queries in the recovered control domain. */
public final class MovementSimulation implements MinecraftAccess {
    private static LocalPlayer sampledPlayer;
    private static long sampledAge = Long.MIN_VALUE;
    private static final Map<Integer, List<FallSample>> samplesByGlideTicks = new HashMap<>();

    private MovementSimulation() {
    }

    public static double attackRange() {
        return CLIENT.player == null ? 3.0 : CLIENT.player.entityInteractionRange();
    }

    public static List<FallSample> fallSamples(int glideTicks) {
        LocalPlayer player = CLIENT.player;
        if (player == null || CLIENT.level == null) {
            return List.of();
        }
        if (player != sampledPlayer || player.tickCount != sampledAge) {
            samplesByGlideTicks.clear();
            sampledPlayer = player;
            sampledAge = player.tickCount;
        }
        return samplesByGlideTicks.computeIfAbsent(Math.max(0, glideTicks),
                ticks -> sampleFall(player, ticks));
    }

    private static List<FallSample> sampleFall(LocalPlayer player, int glideTicks) {
        List<FallSample> samples = new ArrayList<>(21);
        samples.add(new FallSample(0, player.getEyePosition(), player.onGround(), player.isFallFlying(), player.fallDistance));
        if (player.onGround() || CLIENT.level == null) {
            return samples;
        }
        // The original factory call is outside the recovery catch.
        SimulatedPlayer simulation = PlayerPrediction.forWorld(CLIENT.level);
        try {
            simulation.copyFrom(player, player.getDeltaMovement(), player.getYRot(), player.getXRot(),
                    player.zza, player.xxa, player.input.keyPresses.jump());
            double fallDistance = player.fallDistance;
            double previousY = player.getY();
            for (int tick = 1; tick <= 20; tick++) {
                if (simulation.isFallFlying()) {
                    if (simulation.getDeltaMovement().y > -0.5 && fallDistance > 1.0) {
                        fallDistance = 1.0;
                    }
                    if (tick > glideTicks) {
                        ((EntityAccessor) simulation).invokeSetFlag(7, false);
                        simulation.setPose(Pose.STANDING);
                        simulation.refreshDimensions();
                    }
                }
                simulation.tickPrediction();
                double currentY = simulation.getY();
                if (!simulation.isInWater() && currentY < previousY) {
                    fallDistance += previousY - currentY;
                }
                previousY = currentY;
                boolean onGround = simulation.onGround();
                samples.add(new FallSample(tick, simulation.getEyePosition(), onGround,
                        simulation.isFallFlying(), onGround ? 0.0 : fallDistance));
                if (onGround) {
                    break;
                }
            }
        } catch (Throwable failure) {
            // An actual original failure path: discard partial predictions, keep sample zero.
            return List.of(samples.get(0));
        }
        return samples;
    }

    public static AABB sampleBounds(List<FallSample> samples) {
        AABB bounds = new AABB(samples.get(0).eyePosition(), samples.get(0).eyePosition());
        for (FallSample sample : samples) {
            bounds = bounds.minmax(new AABB(sample.eyePosition(), sample.eyePosition()));
        }
        return bounds;
    }

    public static boolean isHorizontallyReachable(List<FallSample> samples, LivingEntity target, double distance) {
        CollisionPrediction prediction = collisionPrediction(target);
        double distanceSquared = distance * distance;
        for (FallSample sample : samples) {
            AABB bounds = prediction.predictClamped(sample.tick());
            double nearestX = Math.max(bounds.minX, Math.min(sample.eyePosition().x, bounds.maxX));
            double nearestZ = Math.max(bounds.minZ, Math.min(sample.eyePosition().z, bounds.maxZ));
            double x = sample.eyePosition().x - nearestX;
            double z = sample.eyePosition().z - nearestZ;
            if (x * x + z * z <= distanceSquared) {
                return true;
            }
        }
        return false;
    }

    public static int firstImpactTick(List<FallSample> samples, LivingEntity target, double minimumFallDistance) {
        CollisionPrediction prediction = collisionPrediction(target);
        for (FallSample sample : samples) {
            if (sample.isFalling() && sample.fallDistance() > minimumFallDistance && withinAttackRange(sample, prediction)) {
                return sample.tick();
            }
        }
        return -1;
    }

    private static boolean withinAttackRange(FallSample sample, CollisionPrediction prediction) {
        double range = attackRange() * (sample.tick() == 0 ? 1.0 : 0.9);
        return prediction.predictClamped(sample.tick()).distanceToSqr(sample.eyePosition()) <= range * range;
    }

    public static boolean canHitWithinWindow(List<FallSample> samples, LivingEntity target,
            double minimumFallDistance, double horizontalDistance, TickWindow window) {
        CollisionPrediction prediction = collisionPrediction(target);
        return canHitWithinWindow(samples, prediction, minimumFallDistance, horizontalDistance, attackRange(), window);
    }

    private static boolean canHitWithinWindow(List<FallSample> samples, CollisionPrediction prediction,
            double minimumFallDistance, double horizontalDistance, double attackRange, TickWindow window) {
        for (FallSample first : samples) {
            if (first.tick() < window.firstTick() || first.tick() + window.pairedTickOffset() > window.lastTick()) {
                continue;
            }
            if (!canHit(first, prediction, minimumFallDistance, horizontalDistance, attackRange)) {
                continue;
            }
            if (window.pairedTickOffset() == 0) {
                return true;
            }
            for (FallSample paired : samples) {
                if (paired.tick() == first.tick() + window.pairedTickOffset()
                        && canHit(paired, prediction, minimumFallDistance, horizontalDistance, attackRange)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean canHit(FallSample sample, CollisionPrediction prediction,
            double minimumFallDistance, double horizontalDistance, double attackRange) {
        // This original helper uses DCMPG (unlike firstImpactTick's DCMPL).
        if (!sample.isFalling() || sample.fallDistance() <= minimumFallDistance) {
            return false;
        }
        AABB bounds = prediction.predict(sample.tick());
        double range = attackRange * 0.9;
        if (bounds.distanceToSqr(sample.eyePosition()) > range * range) {
            return false;
        }
        double x = sample.eyePosition().x - Math.max(bounds.minX, Math.min(sample.eyePosition().x, bounds.maxX));
        double z = sample.eyePosition().z - Math.max(bounds.minZ, Math.min(sample.eyePosition().z, bounds.maxZ));
        return x * x + z * z <= horizontalDistance * horizontalDistance;
    }

    public static Vec3 predictedAimPoint(LivingEntity target, int ticks) {
        AABB bounds = collisionPrediction(target).predictClamped(Math.max(0, ticks));
        return new Vec3((bounds.minX + bounds.maxX) * 0.5,
                bounds.minY + (bounds.maxY - bounds.minY) * 0.75,
                (bounds.minZ + bounds.maxZ) * 0.5);
    }

    private static CollisionPrediction collisionPrediction(LivingEntity target) {
        EntityHitboxEvent event = new EntityHitboxEvent(target, target.getBoundingBox().inflate(target.getPickRadius()));
        event.post();
        Vec3 measuredVelocity = target.position().subtract(target.xo, target.yo, target.zo);
        InterpolationHandler interpolator = target.getInterpolation();
        Vec3 interpolationDisplacement = Vec3.ZERO;
        if (interpolator != null && interpolator.hasActiveInterpolation()) {
            interpolationDisplacement = interpolator.position().subtract(target.position());
        }
        return CollisionPrediction.create(event.getBoundingBox(), measuredVelocity, target.getDeltaMovement(),
                interpolationDisplacement, target.onGround());
    }
}
