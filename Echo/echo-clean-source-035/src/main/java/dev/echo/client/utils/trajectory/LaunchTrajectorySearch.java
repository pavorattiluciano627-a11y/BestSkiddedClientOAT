package dev.echo.client.utils.trajectory;

import dev.echo.client.MinecraftAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Seeded launch/interception search reconstructed in its documented key/cache domain. */
public final class LaunchTrajectorySearch implements MinecraftAccess {
    private static final double[][] LAUNCH_SPREAD = TrajectoryNumerics.launchSpread(16, 0x5eed1L);
    private static final double[][] PROJECTILE_SPREAD = TrajectoryNumerics.launchSpread(16, 0x5eed2L);
    private static final float[] COARSE_PITCH = {-3.0F, -2.0F, -1.0F, 0.0F, 1.0F, 2.0F, 3.0F};
    private static final float[] FINE_PITCH = {-0.5F, -0.25F, 0.0F, 0.25F, 0.5F};
    private static final float[] FINE_YAW = {-0.5F, 0.0F, 0.5F};

    private LaunchTrajectorySearch() {
    }

    public static int flightTicks(ProjectileState state) {
        double x = state.position().x;
        double y = state.position().y;
        double z = state.position().z;
        double velocityX = state.velocity().x;
        double velocityY = state.velocity().y;
        double velocityZ = state.velocity().z;
        for (int tick = 1; tick <= 80; tick++) {
            velocityY -= 0.03;
            velocityX *= 0.99;
            velocityY *= 0.99;
            velocityZ *= 0.99;
            Vec3 start = new Vec3(x, y, z);
            Vec3 end = new Vec3(x + velocityX, y + velocityY, z + velocityZ);
            if (CLIENT.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, CLIENT.player)).getType() != HitResult.Type.MISS) {
                return tick;
            }
            x = end.x;
            y = end.y;
            z = end.z;
        }
        return 81;
    }

    public static LaunchCandidate findCatch(ProjectileState state, Vec3 origin, Vec3 velocity,
            int elapsedTicks, int flightTicks, boolean lowTrajectory) {
        int finalTick = Math.min(80, flightTicks - 1);
        double[][] path = TrajectoryNumerics.sampleProjectile(state, finalTick);
        List<AngleSeed> seeds = new ArrayList<>();
        for (int delay = Math.max(0, elapsedTicks - 1); delay <= elapsedTicks + 1; delay++) {
            collectAngles(path, origin, velocity, delay, finalTick, seeds);
        }
        List<LaunchCandidate> candidates = new ArrayList<>();
        for (AngleSeed seed : seeds) {
            LaunchCandidate candidate = refine(state, seed, origin, velocity, elapsedTicks, finalTick);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }
        LaunchCandidate best = null;
        for (LaunchCandidate candidate : candidates) {
            if (best == null || candidate.robustnessScore() > best.robustnessScore() + 1.0E-9) {
                best = candidate;
            }
        }
        if (best == null) {
            return null;
        }
        if (lowTrajectory) {
            // Original preference is an earlier mean hit within the score tolerance.
            for (LaunchCandidate candidate : candidates) {
                if (candidate.robustnessScore() >= best.robustnessScore() - 0.05
                        && candidate.meanHitTick() < best.meanHitTick()) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    public static int interceptTick(ProjectileState state, Vec3 position, Vec3 velocity,
            int launchDelay, int finalTick) {
        return sampledInterceptTick(state, 0, position, velocity, launchDelay, Math.min(80, finalTick));
    }

    public static double hitFraction(ProjectileState state, Vec3 origin, Vec3 velocity,
            int elapsedTicks, float yaw, float pitch, int finalTick) {
        Vec3 direction = LaunchTrajectoryMath.direction(yaw, pitch);
        int hits = 0;
        for (int sample = 0; sample < 16; sample++) {
            if (sampledInterceptTick(state, sample, origin, launchVelocity(direction, velocity, sample),
                    elapsedTicks, finalTick) > 0) {
                hits++;
            }
        }
        return hits / 16.0;
    }

    private static void collectAngles(double[][] path, Vec3 origin, Vec3 velocity,
            int launchDelay, int finalTick, List<AngleSeed> seeds) {
        for (int tick = launchDelay + 2; tick <= finalTick; tick++) {
            int launchAge = tick - 1 - launchDelay;
            double[] start = path[tick - 1];
            double[] end = path[tick];
            double previousResidual = Double.NaN;
            for (int division = 0; division <= 10; division++) {
                double fraction = division / 10.0;
                double residual = LaunchTrajectoryMath.reachResidual(start, end, fraction, origin, velocity, launchAge);
                if (division > 0 && (Math.signum(residual) != Math.signum(previousResidual) || Math.abs(residual) < 0.02)) {
                    double crossing = TrajectoryNumerics.bisectReach(start, end, (division - 1) / 10.0,
                            fraction, origin, velocity, launchAge);
                    Vec3 point = new Vec3(start[0] + (end[0] - start[0]) * crossing,
                            start[1] + (end[1] - start[1]) * crossing,
                            start[2] + (end[2] - start[2]) * crossing);
                    Vec3 relative = point.subtract(origin).subtract(velocity.scale(launchAge));
                    if (!(relative.lengthSqr() > 1.0E-8)) {
                        break;
                    }
                    Vec3 direction = relative.normalize();
                    float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
                    float pitch = (float) Math.toDegrees(-Math.asin(Mth.clamp(direction.y, -1.0, 1.0)));
                    boolean duplicate = false;
                    for (AngleSeed existing : seeds) {
                        if (Math.abs(Mth.wrapDegrees(existing.yaw() - yaw)) < 1.0F
                                && Math.abs(existing.pitch() - pitch) < 1.0F) {
                            duplicate = true;
                        }
                    }
                    if (!duplicate) {
                        seeds.add(new AngleSeed(yaw, pitch, (float) tick));
                    }
                    break;
                }
                previousResidual = residual;
            }
        }
    }

    private static LaunchCandidate refine(ProjectileState state, AngleSeed seed, Vec3 origin, Vec3 velocity,
            int launchDelay, int finalTick) {
        int horizon = Math.min(finalTick, (int) seed.hitTick() + 10);
        LaunchCandidate coarse = null;
        for (float pitchOffset : COARSE_PITCH) {
            coarse = better(coarse, evaluate(state, seed.yaw(), seed.pitch() + pitchOffset, origin, velocity, launchDelay, horizon));
        }
        if (coarse == null) {
            return null;
        }
        LaunchCandidate best = coarse;
        for (float yawOffset : FINE_YAW) {
            for (float pitchOffset : FINE_PITCH) {
                if (yawOffset == 0.0F && pitchOffset == 0.0F) {
                    continue;
                }
                best = better(best, evaluate(state, coarse.yaw() + yawOffset, coarse.pitch() + pitchOffset,
                        origin, velocity, launchDelay, horizon));
            }
        }
        return isPathClear(origin, velocity, best, launchDelay) ? best : null;
    }

    private static LaunchCandidate better(LaunchCandidate current, LaunchCandidate candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null || candidate.robustnessScore() > current.robustnessScore() ? candidate : current;
    }

    private static LaunchCandidate evaluate(ProjectileState state, float yaw, float pitch,
            Vec3 origin, Vec3 velocity, int launchDelay, int finalTick) {
        pitch = Mth.clamp(pitch, -90.0F, 90.0F);
        Vec3 direction = LaunchTrajectoryMath.direction(yaw, pitch);
        int hits = 0;
        int earlierHits = 0;
        int laterHits = 0;
        int hitTickSum = 0;
        for (int sample = 0; sample < 16; sample++) {
            Vec3 initialVelocity = launchVelocity(direction, velocity, sample);
            int hit = sampledInterceptTick(state, sample, origin, initialVelocity, launchDelay, finalTick);
            int earlier = sampledInterceptTick(state, sample, origin, initialVelocity, launchDelay - 1, finalTick);
            int later = sampledInterceptTick(state, sample, origin, initialVelocity, launchDelay + 1, finalTick);
            if (hit > 0) {
                hits++;
                hitTickSum += hit;
            }
            if (earlier > 0) {
                earlierHits++;
            }
            if (later > 0) {
                laterHits++;
            }
        }
        if (hits == 0) {
            return null;
        }
        double fraction = hits / 16.0;
        double score = fraction + (0.5 * earlierHits + 0.7 * laterHits) / 16.0;
        return new LaunchCandidate(yaw, pitch, fraction, score, Math.round((float) hitTickSum / (float) hits));
    }

    private static boolean isPathClear(Vec3 origin, Vec3 inheritedVelocity, LaunchCandidate candidate, int launchDelay) {
        Vec3 velocity = LaunchTrajectoryMath.direction(candidate.yaw(), candidate.pitch()).scale(1.5).add(inheritedVelocity);
        Vec3 end = origin.add(velocity.scale(Math.max(1, candidate.meanHitTick() - launchDelay)));
        return CLIENT.level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CLIENT.player)).getType() == HitResult.Type.MISS;
    }

    private static int sampledInterceptTick(ProjectileState state, int sample, Vec3 origin, Vec3 velocity,
            int launchDelay, int finalTick) {
        if (launchDelay < 0) {
            return -1;
        }
        double x = state.position().x;
        double y = state.position().y;
        double z = state.position().z;
        double velocityX = state.velocity().x;
        double velocityY = state.velocity().y;
        double velocityZ = state.velocity().z;
        if (!state.exactVelocity()) {
            velocityX += PROJECTILE_SPREAD[sample][0] * 1.5;
            velocityY += PROJECTILE_SPREAD[sample][1] * 1.5;
            velocityZ += PROJECTILE_SPREAD[sample][2] * 1.5;
        }
        for (int tick = 1; tick <= finalTick; tick++) {
            velocityY -= 0.03;
            velocityX *= 0.99;
            velocityY *= 0.99;
            velocityZ *= 0.99;
            if (tick > launchDelay) {
                int launchAge = tick - 1 - launchDelay;
                double centerX = origin.x + velocity.x * launchAge;
                double centerY = origin.y + velocity.y * launchAge;
                double centerZ = origin.z + velocity.z * launchAge;
                double expansion = TrajectoryTolerance.collisionExpansion(tick);
                if (SegmentBoxIntersection.intersects(x, y, z, velocityX, velocityY, velocityZ,
                        centerX - 0.15625 - expansion, centerY - 0.15 - expansion, centerZ - 0.15625 - expansion,
                        centerX + 0.15625 + expansion, centerY - 0.15 + 0.3125 + expansion, centerZ + 0.15625 + expansion)) {
                    return tick;
                }
            }
            x += velocityX;
            y += velocityY;
            z += velocityZ;
        }
        return -1;
    }

    private static Vec3 launchVelocity(Vec3 direction, Vec3 inheritedVelocity, int sample) {
        return new Vec3((direction.x + LAUNCH_SPREAD[sample][0]) * 1.5 + inheritedVelocity.x,
                (direction.y + LAUNCH_SPREAD[sample][1]) * 1.5 + inheritedVelocity.y,
                (direction.z + LAUNCH_SPREAD[sample][2]) * 1.5 + inheritedVelocity.z);
    }

    private record AngleSeed(float yaw, float pitch, float hitTick) {
    }
}
