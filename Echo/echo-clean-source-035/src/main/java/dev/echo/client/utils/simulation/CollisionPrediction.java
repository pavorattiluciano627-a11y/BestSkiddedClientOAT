package dev.echo.client.utils.simulation;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Candidate collision prediction; branch and field-target obligations are in needs.json. */
public record CollisionPrediction(AABB bounds, Vec3 velocity, boolean onGround, Vec3 displacement) {
    public static CollisionPrediction create(AABB bounds, Vec3 measuredVelocity,
            Vec3 expectedVelocity, Vec3 displacement, boolean onGround) {
        double threshold = 8.0 + expectedVelocity.length() * 3.0;
        boolean discontinuity = measuredVelocity.distanceToSqr(expectedVelocity) >= 16.0
                || displacement.lengthSqr() > threshold * threshold;
        return discontinuity
                ? new CollisionPrediction(bounds, Vec3.ZERO, onGround, displacement)
                : new CollisionPrediction(bounds, measuredVelocity, onGround, Vec3.ZERO);
    }

    public AABB predictClamped(int ticks) {
        return translated(ticks, Math.min(ticks, 5), onGround ? 0.0 : Math.min(ticks, 3));
    }

    public AABB predict(int ticks) {
        return translated(ticks, ticks, onGround ? 0.0 : ticks);
    }

    private AABB translated(int ticks, double horizontalTicks, double verticalTicks) {
        if (ticks == 0) {
            return bounds;
        }
        return bounds.move(displacement.add(velocity.x * horizontalTicks,
                velocity.y * verticalTicks, velocity.z * horizontalTicks));
    }
}
