package dev.echo.client.utils.trajectory;

import java.util.Random;
import net.minecraft.util.math.Vec3d;

/** Candidate numerical bodies, pending the branch invariants documented in needs.json. */
public final class TrajectoryNumerics {
    private TrajectoryNumerics() {
    }

    public static double[][] sampleProjectile(ProjectileState state, int ticks) {
        double[][] positions = new double[ticks + 1][];
        double x = state.position().x;
        double y = state.position().y;
        double z = state.position().z;
        double velocityX = state.velocity().x;
        double velocityY = state.velocity().y;
        double velocityZ = state.velocity().z;
        positions[0] = new double[] {x, y, z};
        for (int tick = 1; tick <= ticks; tick++) {
            velocityY -= 0.03;
            velocityX *= 0.99;
            velocityY *= 0.99;
            velocityZ *= 0.99;
            x += velocityX;
            y += velocityY;
            z += velocityZ;
            positions[tick] = new double[] {x, y, z};
        }
        return positions;
    }

    public static double bisectReach(double[] start, double[] end, double lower,
            double upper, Vec3d launchPosition, Vec3d inheritedVelocity, int ticks) {
        double lowerResidual = LaunchTrajectoryMath.reachResidual(
                start, end, lower, launchPosition, inheritedVelocity, ticks);
        for (int iteration = 0; iteration < 24; iteration++) {
            double middle = (lower + upper) * 0.5;
            double residual = LaunchTrajectoryMath.reachResidual(
                    start, end, middle, launchPosition, inheritedVelocity, ticks);
            if (Math.signum(residual) == Math.signum(lowerResidual)) {
                lower = middle;
                lowerResidual = residual;
            } else {
                upper = middle;
            }
        }
        return (lower + upper) * 0.5;
    }

    public static double[][] launchSpread(int count, long randomSeed) {
        Random random = new Random(randomSeed);
        double[][] spread = new double[count][3];
        for (int sample = 1; sample < count; sample++) {
            for (int axis = 0; axis < 3; axis++) {
                spread[sample][axis] = 0.0172275 * (random.nextDouble() - random.nextDouble());
            }
        }
        return spread;
    }

    public static Vec3d[] movingTargetSpread() {
        net.minecraft.util.math.random.Random random = net.minecraft.util.math.random.Random.create(436193L);
        Vec3d[] spread = new Vec3d[16];
        spread[0] = Vec3d.ZERO;
        for (int sample = 1; sample < 16; sample++) {
            spread[sample] = new Vec3d(random.nextTriangular(0.0, 0.0172275),
                    random.nextTriangular(0.0, 0.0172275), random.nextTriangular(0.0, 0.0172275));
        }
        return spread;
    }
}
