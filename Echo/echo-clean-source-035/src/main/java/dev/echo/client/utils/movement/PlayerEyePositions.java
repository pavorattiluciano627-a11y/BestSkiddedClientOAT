package dev.echo.client.utils.movement;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/** The lowered eye origin used by the original player-motion helper. */
public final class PlayerEyePositions {
    private PlayerEyePositions() {}

    // Original member identity and bytecode evidence: symbols.json, getWorker.
    public static Vec3d loweredEyePosition(PlayerEntity player) {
        return new Vec3d(player.getX(), player.getEyeY() - 0.1, player.getZ());
    }
}
