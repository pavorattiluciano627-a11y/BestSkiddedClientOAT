package dev.echo.client.utils.simulation;

import dev.echo.client.MinecraftAccess;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Complete functional candidate; original dispatch and guard domains remain explicit obligations. */
public class PlayerPrediction implements MinecraftAccess {
    private static SimulatedPlayer simulationPlayer;
    private static ClientWorld simulationWorld;

    public static SimulatedPlayer forWorld(ClientWorld world) {
        if (simulationPlayer == null || simulationWorld != world) {
            simulationPlayer = new SimulatedPlayer(world);
            simulationWorld = world;
        }
        return simulationPlayer;
    }

    public static Vec3d predictDisplacement(PlayerEntity player, float forwardInput, float sidewaysInput) {
        SimulatedPlayer simulation = forWorld((ClientWorld) player.getEntityWorld());
        simulation.copyFrom(player, player.getVelocity(), player.getYaw(), player.getPitch(),
                forwardInput, sidewaysInput, false);
        simulation.setSneaking(false);
        simulation.tickPrediction();
        return simulation.getEntityPos().subtract(player.getEntityPos());
    }
}
