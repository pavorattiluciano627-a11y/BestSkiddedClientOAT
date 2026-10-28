package dev.echo.client.feature.combat;

import net.minecraft.entity.LivingEntity;

/** A target and the first simulated tick at which it can be struck.
 * @param target predicted target
 * @param impactTick index in the simulated path
 */
public record MaceImpactPrediction(LivingEntity target, int impactTick) {
}
