package dev.echo.client.utils.trajectory;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Samples the original bow simulation, including a zero-pull launch. */
public final class BowTrajectories {
    private BowTrajectories() {}

    public static List<Vec3> fromPlayer(Player player, int useTicks, float tickDelta) {
        if (player == null || Minecraft.getInstance().level == null) {
            return Collections.emptyList();
        }
        BowSimulation simulation = new BowSimulation(player, Math.max(0, useTicks), tickDelta);
        return TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step);
    }
}
