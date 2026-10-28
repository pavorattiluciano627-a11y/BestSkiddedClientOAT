package dev.echo.client.utils.trajectory;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Samples the original bow simulation, including a zero-pull launch. */
public final class BowTrajectories {
    private BowTrajectories() {}

    public static List<Vec3d> fromPlayer(PlayerEntity player, int useTicks, float tickDelta) {
        if (player == null || MinecraftClient.getInstance().world == null) {
            return Collections.emptyList();
        }
        BowSimulation simulation = new BowSimulation(player, Math.max(0, useTicks), tickDelta);
        return TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step);
    }
}
