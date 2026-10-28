package dev.echo.client.feature.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Recovered pearl-catch aiming, motion and fallback-use operations. */
public final class PearlCatchCalculations {
    private PearlCatchCalculations() {
    }

    /** Computes the bounded angular speed required before the scheduled throw.
     * @param targetYaw planned yaw
     * @param targetPitch planned pitch
     * @param playerYaw current visible yaw
     * @param playerPitch current visible pitch
     * @param throwTick scheduled player-age tick
     * @param playerAge current player age
     * @return angular speed, clamped to 40–300 degrees per second
     */
    public static float aimSpeed(float targetYaw, float targetPitch, float playerYaw, float playerPitch,
                                 int throwTick, int playerAge) {
        float yawDifference = Math.abs(Mth.wrapDegrees(targetYaw - playerYaw));
        float pitchDifference = Math.abs(targetPitch - playerPitch);
        float largestDifference = Math.max(yawDifference, pitchDifference);
        float availableSeconds = Math.max(1, throwTick - playerAge - 1) * 0.05F;
        return Mth.clamp(largestDifference / availableSeconds, 40.0F, 300.0F);
    }

    /** Selects the captured origin while the sequence has one.
     * @param capturedOrigin origin captured by the sequence, or {@code null}
     * @param playerPosition current player position
     * @return captured origin or current position
     */
    public static Vec3 origin(Vec3 capturedOrigin, Vec3 playerPosition) {
        return capturedOrigin != null ? capturedOrigin : playerPosition;
    }

    /** Removes vertical velocity when the sequence's vertical-suppression flag is set.
     * @param savedVelocity velocity captured by the sequence
     * @param suppressVertical whether to replace the vertical component with zero
     * @return a new velocity vector
     */
    public static Vec3 restoredVelocity(Vec3 savedVelocity, boolean suppressVertical) {
        return new Vec3(savedVelocity.x, suppressVertical ? 0.0 : savedVelocity.y, savedVelocity.z);
    }

    /** Uses the offhand wind charge when normal input used only the pearl.
     * @param client client with a live player and interaction manager
     */
    public static void usePendingOffhandWindCharge(Minecraft client) {
        if (client.player.getCooldowns().isOnCooldown(new ItemStack(Items.ENDER_PEARL))
                && !client.player.getCooldowns().isOnCooldown(new ItemStack(Items.WIND_CHARGE))
                && client.player.getOffhandItem().is(Items.WIND_CHARGE)) {
            client.gameMode.useItem(client.player, InteractionHand.OFF_HAND);
        }
    }
}
