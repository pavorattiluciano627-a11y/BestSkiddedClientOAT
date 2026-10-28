package dev.echo.client.utils.trajectory;

import net.minecraft.world.phys.Vec3;

/** Candidate launch state; the flag suppresses added velocity spread. */
public record ProjectileState(Vec3 position, Vec3 velocity, boolean exactVelocity) {
}
