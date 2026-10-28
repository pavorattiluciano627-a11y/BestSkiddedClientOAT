package dev.echo.client.handlers;

import net.minecraft.world.phys.Vec3;

/** A remote player's server-tracked position and grounded state at a tracker tick. */
public record PositionSample(int tick, Vec3 position, boolean onGround) {
}
