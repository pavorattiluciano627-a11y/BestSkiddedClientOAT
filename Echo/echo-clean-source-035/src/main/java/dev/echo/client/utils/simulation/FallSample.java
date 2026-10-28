package dev.echo.client.utils.simulation;

import net.minecraft.world.phys.Vec3;

/** Candidate sampled player state; position is the eye position, not velocity. */
public record FallSample(int tick, Vec3 eyePosition, boolean onGround,
        boolean gliding, double fallDistance) {
    public boolean isFalling() {
        return !onGround && !gliding;
    }
}
