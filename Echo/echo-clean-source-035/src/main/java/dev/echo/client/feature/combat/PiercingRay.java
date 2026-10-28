package dev.echo.client.feature.combat;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/** A player and the eye-to-reach segment used for a piercing pick.
 * @param player player performing the pick
 * @param start camera position
 * @param end end of the look segment
 */
public record PiercingRay(ClientPlayerEntity player, Vec3d start, Vec3d end) {
}
