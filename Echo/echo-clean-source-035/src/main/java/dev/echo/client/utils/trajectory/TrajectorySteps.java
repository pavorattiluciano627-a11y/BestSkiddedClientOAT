package dev.echo.client.utils.trajectory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.world.phys.Vec3;

/** Trajectory sampling under the original null-guard branch; see needs.json. */
public final class TrajectorySteps {
    private TrajectorySteps() {}

    public static List<Vec3> collect(int maxSteps, Supplier<Vec3> position,
                                      BooleanSupplier stopped, Runnable step) {
        List<Vec3> points = new ArrayList<>(maxSteps + 1);
        points.add(position.get());
        for (int tick = 0; tick < maxSteps && !stopped.getAsBoolean(); tick++) {
            step.run();
            points.add(position.get());
        }
        return points;
    }
}
