package dev.echo.client.utils.trajectory;

import dev.echo.client.Echo;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Potion trajectory factories in the documented original operation/key domain. */
public final class ThrownProjectileTrajectories {
    private static final float PITCH_OFFSET = -20.0F;
    private static final double LAUNCH_SPEED = 0.5;
    private static final double EYE_ORIGIN_OFFSET = 0.1;
    private static final double MOVEMENT_EPSILON_SQUARED = 4.0E-8;
    private static final int MAX_STEPS = 80;

    private ThrownProjectileTrajectories() {}

    /**
     * Simulates the player's potion throw. The original accepts {@code tickDelta}
     * but never reads it; current position/angles are used without interpolation.
     */
    public static List<Vec3d> fromPlayer(PlayerEntity player, float tickDelta) {
        if (player == null || MinecraftClient.getInstance().world == null) {
            return Collections.emptyList();
        }
        Vec3d position = new Vec3d(player.getX(), player.getEyeY() - EYE_ORIGIN_OFFSET, player.getZ());
        Vec3d velocity = launchDirection(player, PITCH_OFFSET).multiply(LAUNCH_SPEED)
                .add(inheritedMovement(player));
        ThrownProjectileSimulation simulation = new ThrownProjectileSimulation(position, velocity, player);
        return TrajectorySteps.collect(MAX_STEPS, simulation::getPosition, simulation::isStopped, simulation::step);
    }

    public static List<Vec3d> fromState(Vec3d position, Vec3d velocity) {
        if (position == null || velocity == null || MinecraftClient.getInstance().world == null) {
            return Collections.emptyList();
        }
        ThrownProjectileSimulation simulation = new ThrownProjectileSimulation(position, velocity, null);
        return TrajectorySteps.collect(MAX_STEPS, simulation::getPosition, simulation::isStopped, simulation::step);
    }

    public static List<Vec3d> fromEntity(Entity entity) {
        if (entity == null || MinecraftClient.getInstance().world == null) {
            return Collections.emptyList();
        }
        ThrownProjectileSimulation simulation = new ThrownProjectileSimulation(entity.getEntityPos(), entity.getVelocity(), entity);
        return TrajectorySteps.collect(MAX_STEPS, simulation::getPosition, simulation::isStopped, simulation::step);
    }

    /** The three BatchManager launch operations are expanded here; see the API receipt. */
    private static Vec3d launchDirection(PlayerEntity player, float pitchOffset) {
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        if (player == MinecraftClient.getInstance().player
                && Echo.handlerManager.getRotationManager().isSilentRotationActive()) {
            // Separate provider reads preserve the original yaw-then-pitch access order.
            yaw = Echo.handlerManager.getRotationManager().effectiveRotation().yaw();
            pitch = Echo.handlerManager.getRotationManager().effectiveRotation().pitch();
        }
        return Vec3d.fromPolar(pitch + pitchOffset, yaw).normalize();
    }

    private static Vec3d inheritedMovement(PlayerEntity player) {
        Vec3d movement = player.getEntityPos().subtract(player.lastX, player.lastY, player.lastZ);
        if (movement.lengthSquared() <= MOVEMENT_EPSILON_SQUARED) {
            return Vec3d.ZERO;
        }
        return player.isOnGround() ? new Vec3d(movement.x, 0.0, movement.z) : movement;
    }
}
