package dev.echo.client.utils.trajectory;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** The key-independent numerical operations of the launch-trajectory search. */
public final class LaunchTrajectoryMath {
    private LaunchTrajectoryMath() {
    }

    /**
     * Direction for yaw and pitch in degrees. Float products and Yarn's sine
     * table are intentional: replacing them with double trigonometry changes
     * the search's sampled trajectories.
     */
    public static Vec3d direction(float yaw, float pitch) {
        float x = -MathHelper.sin(yaw * (float) (Math.PI / 180.0))
                * MathHelper.cos(pitch * (float) (Math.PI / 180.0));
        float y = -MathHelper.sin(pitch * (float) (Math.PI / 180.0));
        float z = MathHelper.cos(yaw * (float) (Math.PI / 180.0))
                * MathHelper.cos(pitch * (float) (Math.PI / 180.0));
        return new Vec3d(x, y, z).normalize();
    }

    /** Signed distance from an interpolated path point to the launch reach. */
    public static double reachResidual(double[] segmentStart, double[] segmentEnd,
            double fraction, Vec3d launchPosition, Vec3d inheritedVelocity, int ticks) {
        double x = segmentStart[0] + (segmentEnd[0] - segmentStart[0]) * fraction
                - launchPosition.x - inheritedVelocity.x * ticks;
        double y = segmentStart[1] + (segmentEnd[1] - segmentStart[1]) * fraction
                - launchPosition.y - inheritedVelocity.y * ticks;
        double z = segmentStart[2] + (segmentEnd[2] - segmentStart[2]) * fraction
                - launchPosition.z - inheritedVelocity.z * ticks;
        return Math.sqrt(x * x + y * y + z * z) - 1.5 * ticks;
    }
}
