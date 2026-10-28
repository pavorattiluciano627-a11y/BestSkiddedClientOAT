package dev.echo.client.handlers;

import net.minecraft.util.math.Vec3d;

/** A remote player's server-tracked position and grounded state at a tracker tick. */
public record PositionSample(int tick, Vec3d position, boolean onGround) {
}
