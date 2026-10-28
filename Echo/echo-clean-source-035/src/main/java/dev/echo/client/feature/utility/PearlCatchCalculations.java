package dev.echo.client.feature.utility;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

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
        float yawDifference = Math.abs(MathHelper.wrapDegrees(targetYaw - playerYaw));
        float pitchDifference = Math.abs(targetPitch - playerPitch);
        float largestDifference = Math.max(yawDifference, pitchDifference);
        float availableSeconds = Math.max(1, throwTick - playerAge - 1) * 0.05F;
        return MathHelper.clamp(largestDifference / availableSeconds, 40.0F, 300.0F);
    }

    /** Selects the captured origin while the sequence has one.
     * @param capturedOrigin origin captured by the sequence, or {@code null}
     * @param playerPosition current player position
     * @return captured origin or current position
     */
    public static Vec3d origin(Vec3d capturedOrigin, Vec3d playerPosition) {
        return capturedOrigin != null ? capturedOrigin : playerPosition;
    }

    /** Removes vertical velocity when the sequence's vertical-suppression flag is set.
     * @param savedVelocity velocity captured by the sequence
     * @param suppressVertical whether to replace the vertical component with zero
     * @return a new velocity vector
     */
    public static Vec3d restoredVelocity(Vec3d savedVelocity, boolean suppressVertical) {
        return new Vec3d(savedVelocity.x, suppressVertical ? 0.0 : savedVelocity.y, savedVelocity.z);
    }

    /** Uses the offhand wind charge when normal input used only the pearl.
     * @param client client with a live player and interaction manager
     */
    public static void usePendingOffhandWindCharge(MinecraftClient client) {
        if (client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL))
                && !client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.WIND_CHARGE))
                && client.player.getOffHandStack().isOf(Items.WIND_CHARGE)) {
            client.interactionManager.interactItem(client.player, Hand.OFF_HAND);
        }
    }
}
