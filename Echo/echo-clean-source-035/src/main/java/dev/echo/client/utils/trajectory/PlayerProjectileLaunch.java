package dev.echo.client.utils.trajectory;

import dev.echo.client.Echo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** The two remaining player-motion bodies used by thrown and wind-charge launches. */
public final class PlayerProjectileLaunch {
    private PlayerProjectileLaunch() {}

    public static Vec3 direction(Player player, float pitchOffset) {
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        if (player == Minecraft.getInstance().player
                && Echo.handlerManager.getRotationManager().isSilentRotationActive()) {
            // Retain the two original manager/rotation reads, in yaw-then-pitch order.
            yaw = Echo.handlerManager.getRotationManager().effectiveRotation().yaw();
            pitch = Echo.handlerManager.getRotationManager().effectiveRotation().pitch();
        }
        return Vec3.directionFromRotation(pitch + pitchOffset, yaw).normalize();
    }

    public static Vec3 inheritedMotion(Player player) {
        Vec3 displacement = player.position().subtract(player.xo, player.yo, player.zo);
        if (displacement.lengthSqr() - 4.0E-8 <= 0.0) {
            return Vec3.ZERO;
        }
        return player.onGround() ? new Vec3(displacement.x, 0.0, displacement.z) : displacement;
    }
}
