package dev.echo.client.utils.trajectory;

import dev.echo.client.utils.movement.PlayerEyePositions;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Wind-charge factories retaining the original launch and sampling order. */
public final class WindChargeTrajectories {
    private static final double LAUNCH_SPEED = 1.5;

    private WindChargeTrajectories() {}

    public static List<Vec3> fromPlayer(Player player, float tickDelta) {
        if (player == null || Minecraft.getInstance().level == null) {
            return Collections.emptyList();
        }
        // As in HistoryModel, the accepted tickDelta is unused by the original.
        Vec3 position = PlayerEyePositions.loweredEyePosition(player);
        Vec3 velocity = PlayerProjectileLaunch.direction(player, 0.0F).scale(LAUNCH_SPEED)
                .add(PlayerProjectileLaunch.inheritedMotion(player));
        WindChargeSimulation simulation = new WindChargeSimulation(position, velocity, player);
        return TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step);
    }

    public static List<Vec3> fromVectors(Vec3 position, Vec3 velocity) {
        if (position == null || velocity == null || Minecraft.getInstance().level == null) {
            return Collections.emptyList();
        }
        WindChargeSimulation simulation = new WindChargeSimulation(position, velocity, null);
        return TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step);
    }

    public static List<Vec3> fromEntity(Entity entity) {
        if (entity == null || Minecraft.getInstance().level == null) {
            return Collections.emptyList();
        }
        WindChargeSimulation simulation = new WindChargeSimulation(entity.position(), entity.getDeltaMovement(), entity);
        return TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step);
    }
}
