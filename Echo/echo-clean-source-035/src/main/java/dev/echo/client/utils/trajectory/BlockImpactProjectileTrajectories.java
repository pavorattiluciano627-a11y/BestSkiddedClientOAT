package dev.echo.client.utils.trajectory;

import dev.echo.client.MinecraftAccess;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Factories for the existing block-impact simulator and its ordered trajectory samples. */
public final class BlockImpactProjectileTrajectories implements MinecraftAccess {
    private static final int MAX_STEPS = 80;

    private BlockImpactProjectileTrajectories() {}

    public static List<Vec3> fromState(Vec3 position, Vec3 velocity) {
        if (position == null || velocity == null || CLIENT.level == null) {
            return Collections.emptyList();
        }
        BlockImpactProjectileSimulation simulation = new BlockImpactProjectileSimulation(position, velocity, null);
        return TrajectorySteps.collect(MAX_STEPS, simulation::getPosition, simulation::isStopped, simulation::step);
    }

    public static List<Vec3> fromEntity(Entity entity) {
        if (entity == null || CLIENT.level == null) {
            return Collections.emptyList();
        }
        BlockImpactProjectileSimulation simulation = new BlockImpactProjectileSimulation(
                entity.position(), entity.getDeltaMovement(), entity);
        return TrajectorySteps.collect(MAX_STEPS, simulation::getPosition, simulation::isStopped, simulation::step);
    }
}
