package dev.echo.client.utils.trajectory;

import dev.echo.client.Echo;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

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
    public static List<Vec3> fromPlayer(Player player, float tickDelta) {
        if (player == null || Minecraft.getInstance().level == null) {
            return Collections.emptyList();
        }
        Vec3 position = new Vec3(player.getX(), player.getEyeY() - EYE_ORIGIN_OFFSET, player.getZ());
        Vec3 velocity = launchDirection(player, PITCH_OFFSET).scale(LAUNCH_SPEED)
                .add(inheritedMovement(player));
        ThrownProjectileSimulation simulation = new ThrownProjectileSimulation(position, velocity, player);
        return TrajectorySteps.collect(MAX_STEPS, simulation::getPosition, simulation::isStopped, simulation::step);
    }

    public static List<Vec3> fromState(Vec3 position, Vec3 velocity) {
        if (position == null || velocity == null || Minecraft.getInstance().level == null) {
            return Collections.emptyList();
        }
        ThrownProjectileSimulation simulation = new ThrownProjectileSimulation(position, velocity, null);
        return TrajectorySteps.collect(MAX_STEPS, simulation::getPosition, simulation::isStopped, simulation::step);
    }

    public static List<Vec3> fromEntity(Entity entity) {
        if (entity == null || Minecraft.getInstance().level == null) {
            return Collections.emptyList();
        }
        ThrownProjectileSimulation simulation = new ThrownProjectileSimulation(entity.position(), entity.getDeltaMovement(), entity);
        return TrajectorySteps.collect(MAX_STEPS, simulation::getPosition, simulation::isStopped, simulation::step);
    }

    /** The three BatchManager launch operations are expanded here; see the API receipt. */
    private static Vec3 launchDirection(Player player, float pitchOffset) {
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        if (player == Minecraft.getInstance().player
                && Echo.handlerManager.getRotationManager().isSilentRotationActive()) {
            // Separate provider reads preserve the original yaw-then-pitch access order.
            yaw = Echo.handlerManager.getRotationManager().effectiveRotation().yaw();
            pitch = Echo.handlerManager.getRotationManager().effectiveRotation().pitch();
        }
        return Vec3.directionFromRotation(pitch + pitchOffset, yaw).normalize();
    }

    private static Vec3 inheritedMovement(Player player) {
        Vec3 movement = player.position().subtract(player.xo, player.yo, player.zo);
        if (movement.lengthSqr() <= MOVEMENT_EPSILON_SQUARED) {
            return Vec3.ZERO;
        }
        return player.onGround() ? new Vec3(movement.x, 0.0, movement.z) : movement;
    }
}
