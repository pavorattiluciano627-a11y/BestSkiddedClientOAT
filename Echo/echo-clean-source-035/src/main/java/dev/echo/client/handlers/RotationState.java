package dev.echo.client.handlers;

import dev.echo.client.features.Feature;
import dev.echo.client.utils.Rotation;
import net.minecraft.entity.LivingEntity;

/**
 * Genuine client-services-02 record with only the reconciled Feature package
 * spelling corrected for this analysis compile. No provider behavior is replaced.
 */
public record RotationState(Rotation playerRotation, Rotation currentRotation,
                            Rotation previousRotation, boolean active, Feature owner,
                            LivingEntity target, AimState state) {
}
