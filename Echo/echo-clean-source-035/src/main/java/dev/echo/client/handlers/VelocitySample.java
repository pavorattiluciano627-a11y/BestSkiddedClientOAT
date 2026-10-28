package dev.echo.client.handlers;

import net.minecraft.world.phys.Vec3;

/** A remote player's server-tracked position and received velocity at a tracker tick. */
public record VelocitySample(int tick, Vec3 position, Vec3 velocity) {
}
