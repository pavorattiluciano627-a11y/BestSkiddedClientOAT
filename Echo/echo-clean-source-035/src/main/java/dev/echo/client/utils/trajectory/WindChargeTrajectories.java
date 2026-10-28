package dev.echo.client.utils.trajectory;

import dev.echo.client.utils.movement.PlayerEyePositions;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Wind-charge factories retaining the original launch and sampling order. */
public final class WindChargeTrajectories {
    private static final double LAUNCH_SPEED = 1.5;

    private WindChargeTrajectories() {}

    public static List<Vec3d> fromPlayer(PlayerEntity player, float tickDelta) {
        if (player == null || MinecraftClient.getInstance().world == null) {
            return Collections.emptyList();
        }
        // As in HistoryModel, the accepted tickDelta is unused by the original.
        Vec3d position = PlayerEyePositions.loweredEyePosition(player);
        Vec3d velocity = PlayerProjectileLaunch.direction(player, 0.0F).multiply(LAUNCH_SPEED)
                .add(PlayerProjectileLaunch.inheritedMotion(player));
        WindChargeSimulation simulation = new WindChargeSimulation(position, velocity, player);
        return TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step);
    }

    public static List<Vec3d> fromVectors(Vec3d position, Vec3d velocity) {
        if (position == null || velocity == null || MinecraftClient.getInstance().world == null) {
            return Collections.emptyList();
        }
        WindChargeSimulation simulation = new WindChargeSimulation(position, velocity, null);
        return TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step);
    }

    public static List<Vec3d> fromEntity(Entity entity) {
        if (entity == null || MinecraftClient.getInstance().world == null) {
            return Collections.emptyList();
        }
        WindChargeSimulation simulation = new WindChargeSimulation(entity.getEntityPos(), entity.getVelocity(), entity);
        return TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step);
    }
}
