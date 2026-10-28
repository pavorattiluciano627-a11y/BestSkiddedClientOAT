package dev.echo.client.utils.movement;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** The lowered eye origin used by the original player-motion helper. */
public final class PlayerEyePositions {
    private PlayerEyePositions() {}

    // Original member identity and bytecode evidence: symbols.json, getWorker.
    public static Vec3 loweredEyePosition(Player player) {
        return new Vec3(player.getX(), player.getEyeY() - 0.1, player.getZ());
    }
}
