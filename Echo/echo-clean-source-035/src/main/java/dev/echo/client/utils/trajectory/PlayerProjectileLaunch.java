package dev.echo.client.utils.trajectory;

import dev.echo.client.Echo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/** The two remaining player-motion bodies used by thrown and wind-charge launches. */
public final class PlayerProjectileLaunch {
    private PlayerProjectileLaunch() {}

    public static Vec3d direction(PlayerEntity player, float pitchOffset) {
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        if (player == MinecraftClient.getInstance().player
                && Echo.handlerManager.getRotationManager().isSilentRotationActive()) {
            // Retain the two original manager/rotation reads, in yaw-then-pitch order.
            yaw = Echo.handlerManager.getRotationManager().effectiveRotation().yaw();
            pitch = Echo.handlerManager.getRotationManager().effectiveRotation().pitch();
        }
        return Vec3d.fromPolar(pitch + pitchOffset, yaw).normalize();
    }

    public static Vec3d inheritedMotion(PlayerEntity player) {
        Vec3d displacement = player.getEntityPos().subtract(player.lastX, player.lastY, player.lastZ);
        if (displacement.lengthSquared() - 4.0E-8 <= 0.0) {
            return Vec3d.ZERO;
        }
        return player.isOnGround() ? new Vec3d(displacement.x, 0.0, displacement.z) : displacement;
    }
}
