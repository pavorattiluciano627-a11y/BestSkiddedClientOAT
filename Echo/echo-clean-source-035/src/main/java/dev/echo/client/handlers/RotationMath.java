package dev.echo.client.handlers;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Stateless calculations used when selecting and approaching a rotation target. */
public final class RotationMath {
    private RotationMath() {
    }

    /**
     * Smooths angular velocity toward the requested angular error.
     *
     * @param velocity current angular velocity
     * @param angularError signed remaining angle in degrees
     * @param speed configured speed
     * @param elapsedSeconds elapsed time in seconds
     * @return smoothed velocity limited by the error-dependent speed cap
     */
    public static float smoothVelocity(float velocity, float angularError,
                                       float speed, float elapsedSeconds) {
        float speedLimit = speed * Math.min(1.0F + Math.abs(angularError) / 30.0F, 3.0F);
        float desiredVelocity = MathHelper.clamp(angularError * speed, -speedLimit, speedLimit);
        float blend = 1.0F - (float) Math.exp(-elapsedSeconds * 15.0F);
        return MathHelper.clamp(velocity + (desiredVelocity - velocity) * blend,
                -speedLimit, speedLimit);
    }

    /**
     * Samples each coordinate independently within a bounding box.
     *
     * @param box sampling bounds
     * @return a uniformly sampled point using the current thread's random source
     */
    public static Vec3d randomPoint(Box box) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new Vec3d(
                box.minX + random.nextDouble() * (box.maxX - box.minX),
                box.minY + random.nextDouble() * (box.maxY - box.minY),
                box.minZ + random.nextDouble() * (box.maxZ - box.minZ));
    }
}
