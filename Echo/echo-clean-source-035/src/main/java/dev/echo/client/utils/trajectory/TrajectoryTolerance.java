package dev.echo.client.utils.trajectory;

import net.minecraft.util.Mth;

/** Time-dependent collision expansion used by moving-target trajectory search. */
public final class TrajectoryTolerance {
    private TrajectoryTolerance() {
    }

    public static double collisionExpansion(int ticks) {
        // Subtraction is deliberately performed as an int before division.
        return Mth.clamp((ticks - 2) / 20.0, 0.0, 0.3);
    }
}
