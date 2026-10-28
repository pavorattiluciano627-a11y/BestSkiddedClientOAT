package dev.echo.client.utils.trajectory;

import net.minecraft.util.math.Vec3d;

/** Candidate launch state; the flag suppresses added velocity spread. */
public record ProjectileState(Vec3d position, Vec3d velocity, boolean exactVelocity) {
}
