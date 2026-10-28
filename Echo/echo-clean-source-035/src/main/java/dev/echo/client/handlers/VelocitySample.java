package dev.echo.client.handlers;

import net.minecraft.util.math.Vec3d;

/** A remote player's server-tracked position and received velocity at a tracker tick. */
public record VelocitySample(int tick, Vec3d position, Vec3d velocity) {
}
