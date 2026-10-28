package dev.echo.client.utils.trajectory;

import dev.echo.client.utils.movement.PlayerEyePositions;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Fluid-aware projectile factories; conditional original binding domain is in the receipt. */
public final class FluidAwareProjectileTrajectories {
    private static final double LAUNCH_SPEED = 1.5;

    private FluidAwareProjectileTrajectories() {}

    public static List<Vec3> fromPlayer(Player player, float tickDelta) {
        if (player == null || Minecraft.getInstance().level == null) {
            return Collections.emptyList();
        }
        // The original accepts tickDelta but does not read it in this factory.
        Vec3 position = PlayerEyePositions.loweredEyePosition(player);
        Vec3 velocity = PlayerProjectileLaunch.direction(player, 0.0F).scale(LAUNCH_SPEED)
                .add(PlayerProjectileLaunch.inheritedMotion(player));
        FluidAwareProjectileSimulation simulation = new FluidAwareProjectileSimulation(position, velocity, player);
        return TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step);
    }

    public static List<Vec3> fromVectors(Vec3 position, Vec3 velocity) {
        if (position == null || velocity == null || Minecraft.getInstance().level == null) {
            return Collections.emptyList();
        }
        FluidAwareProjectileSimulation simulation = new FluidAwareProjectileSimulation(position, velocity, null);
        return TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step);
    }

    public static List<Vec3> fromEntity(Entity entity) {
        if (entity == null || Minecraft.getInstance().level == null) {
            return Collections.emptyList();
        }
        FluidAwareProjectileSimulation simulation = new FluidAwareProjectileSimulation(
                entity.position(), entity.getDeltaMovement(), entity);
        return TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step);
    }
}
