package dev.echo.client.utils.simulation;

import net.minecraft.util.math.Vec3d;

/** Candidate sampled player state; position is the eye position, not velocity. */
public record FallSample(int tick, Vec3d eyePosition, boolean onGround,
        boolean gliding, double fallDistance) {
    public boolean isFalling() {
        return !onGround && !gliding;
    }
}
