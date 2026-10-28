package dev.echo.client.feature.combat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** A player and the eye-to-reach segment used for a piercing pick.
 * @param player player performing the pick
 * @param start camera position
 * @param end end of the look segment
 */
public record PiercingRay(LocalPlayer player, Vec3 start, Vec3 end) {
}
