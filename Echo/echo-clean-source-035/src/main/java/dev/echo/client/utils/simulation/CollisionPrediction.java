package dev.echo.client.utils.simulation;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Candidate collision prediction; branch and field-target obligations are in needs.json. */
public record CollisionPrediction(Box bounds, Vec3d velocity, boolean onGround, Vec3d displacement) {
    public static CollisionPrediction create(Box bounds, Vec3d measuredVelocity,
            Vec3d expectedVelocity, Vec3d displacement, boolean onGround) {
        double threshold = 8.0 + expectedVelocity.length() * 3.0;
        boolean discontinuity = measuredVelocity.squaredDistanceTo(expectedVelocity) >= 16.0
                || displacement.lengthSquared() > threshold * threshold;
        return discontinuity
                ? new CollisionPrediction(bounds, Vec3d.ZERO, onGround, displacement)
                : new CollisionPrediction(bounds, measuredVelocity, onGround, Vec3d.ZERO);
    }

    public Box predictClamped(int ticks) {
        return translated(ticks, Math.min(ticks, 5), onGround ? 0.0 : Math.min(ticks, 3));
    }

    public Box predict(int ticks) {
        return translated(ticks, ticks, onGround ? 0.0 : ticks);
    }

    private Box translated(int ticks, double horizontalTicks, double verticalTicks) {
        if (ticks == 0) {
            return bounds;
        }
        return bounds.offset(displacement.add(velocity.x * horizontalTicks,
                velocity.y * verticalTicks, velocity.z * horizontalTicks));
    }
}
