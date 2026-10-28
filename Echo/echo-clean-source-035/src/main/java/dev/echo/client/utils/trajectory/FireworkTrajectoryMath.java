package dev.echo.client.utils.trajectory;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/** Launch-vector and lifetime operations with their real random seeds preserved. */
public final class FireworkTrajectoryMath {
    private FireworkTrajectoryMath() {}

    public static int lifetime(ItemStack stack, long randomSeed) {
        int flight = 1;
        FireworksComponent fireworks = stack.get(DataComponentTypes.FIREWORKS);
        if (fireworks != null) {
            flight += fireworks.flightDuration();
        }
        Random random = Random.create(randomSeed);
        return 10 * flight + random.nextInt(6) + random.nextInt(7);
    }

    public static Vec3d applyDivergence(Vec3d direction, long randomSeed) {
        if (MathHelper.approximatelyEquals(1.0F, 0.0F)) {
            return direction.normalize();
        }
        Random random = Random.create(randomSeed);
        double deviation = 0.0172275;
        Vec3d scattered = direction.normalize().add(random.nextTriangular(0.0, deviation),
                random.nextTriangular(0.0, deviation), random.nextTriangular(0.0, deviation));
        return scattered.normalize();
    }

    public static Vec3d rotateAroundAxis(Vec3d direction, Vec3d axis, float angleDegrees) {
        if (MathHelper.approximatelyEquals(angleDegrees, 0.0F)) {
            return direction;
        }
        Vec3d unitAxis = axis.normalize();
        double radians = angleDegrees * (float) (Math.PI / 180.0);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return direction.multiply(cosine)
                .add(unitAxis.crossProduct(direction).multiply(sine))
                .add(unitAxis.multiply(unitAxis.dotProduct(direction) * (1.0 - cosine)));
    }
}
