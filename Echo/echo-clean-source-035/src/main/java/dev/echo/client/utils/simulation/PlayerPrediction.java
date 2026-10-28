package dev.echo.client.utils.simulation;

import dev.echo.client.MinecraftAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Complete functional candidate; original dispatch and guard domains remain explicit obligations. */
public class PlayerPrediction implements MinecraftAccess {
    private static SimulatedPlayer simulationPlayer;
    private static ClientLevel simulationWorld;

    public static SimulatedPlayer forWorld(ClientLevel world) {
        if (simulationPlayer == null || simulationWorld != world) {
            simulationPlayer = new SimulatedPlayer(world);
            simulationWorld = world;
        }
        return simulationPlayer;
    }

    public static Vec3 predictDisplacement(Player player, float forwardInput, float sidewaysInput) {
        SimulatedPlayer simulation = forWorld((ClientLevel) player.level());
        simulation.copyFrom(player, player.getDeltaMovement(), player.getYRot(), player.getXRot(),
                forwardInput, sidewaysInput, false);
        simulation.setShiftKeyDown(false);
        simulation.tickPrediction();
        return simulation.position().subtract(player.position());
    }
}
