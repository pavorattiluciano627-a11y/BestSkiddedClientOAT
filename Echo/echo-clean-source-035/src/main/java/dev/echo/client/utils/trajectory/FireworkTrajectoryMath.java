package dev.echo.client.utils.trajectory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.phys.Vec3;

/** Launch-vector and lifetime operations with their real random seeds preserved. */
public final class FireworkTrajectoryMath {
    private FireworkTrajectoryMath() {}

    public static int lifetime(ItemStack stack, long randomSeed) {
        int flight = 1;
        Fireworks fireworks = stack.get(DataComponents.FIREWORKS);
        if (fireworks != null) {
            flight += fireworks.flightDuration();
        }
        RandomSource random = RandomSource.create(randomSeed);
        return 10 * flight + random.nextInt(6) + random.nextInt(7);
    }

    public static Vec3 applyDivergence(Vec3 direction, long randomSeed) {
        if (Mth.equal(1.0F, 0.0F)) {
            return direction.normalize();
        }
        RandomSource random = RandomSource.create(randomSeed);
        double deviation = 0.0172275;
        Vec3 scattered = direction.normalize().add(random.triangle(0.0, deviation),
                random.triangle(0.0, deviation), random.triangle(0.0, deviation));
        return scattered.normalize();
    }

    public static Vec3 rotateAroundAxis(Vec3 direction, Vec3 axis, float angleDegrees) {
        if (Mth.equal(angleDegrees, 0.0F)) {
            return direction;
        }
        Vec3 unitAxis = axis.normalize();
        double radians = angleDegrees * (float) (Math.PI / 180.0);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return direction.scale(cosine)
                .add(unitAxis.cross(direction).scale(sine))
                .add(unitAxis.scale(unitAxis.dot(direction) * (1.0 - cosine)));
    }
}
