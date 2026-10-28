package dev.echo.client.utils.trajectory;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.handlers.PositionSample;
import dev.echo.client.handlers.VelocitySample;
import dev.echo.client.utils.Rotation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Motion fitting, collision-aware target prediction and seeded projectile launch search. */
public final class MovingTargetTrajectorySearch implements MinecraftAccess {
    private static final Vec3d[] SPREAD = TrajectoryNumerics.movingTargetSpread();
    private static final float[] ANGLE_OFFSETS = {0.0F, -0.4F, 0.4F, -0.8F, 0.8F};

    private MovingTargetTrajectorySearch() {
    }

    public record Result(Vec3d position, Vec3d velocity, int tick, boolean airborne,
                         boolean reliable, double yawRate, double horizontalDrag) {
    }

    public static Result estimateMotion(PlayerEntity player, List<PositionSample> history,
            VelocitySample receivedVelocity, int tick) {
        if (receivedVelocity != null && tick - receivedVelocity.tick() <= 12) {
            List<PositionSample> recent = history.stream()
                    .filter(sample -> sample.tick() > receivedVelocity.tick()).toList();
            boolean grounded = !recent.isEmpty() && recent.get(recent.size() - 1).onGround();
            if (!grounded && recent.size() < 3) {
                Vec3d position = receivedVelocity.position();
                Vec3d velocity = receivedVelocity.velocity();
                int sampleTick = receivedVelocity.tick();
                if (!recent.isEmpty()) {
                    PositionSample latest = recent.get(recent.size() - 1);
                    Vec3d closestVelocity = velocity;
                    double closestHeight = Double.MAX_VALUE;
                    Vec3d predictedPosition = position;
                    Vec3d predictedVelocity = velocity;
                    for (int step = 0; step <= latest.tick() - receivedVelocity.tick() + 2; step++) {
                        double heightDifference = Math.abs(predictedPosition.y - latest.position().y);
                        if (heightDifference < closestHeight) {
                            closestHeight = heightDifference;
                            closestVelocity = predictedVelocity;
                        }
                        predictedPosition = predictedPosition.add(predictedVelocity);
                        predictedVelocity = new Vec3d(predictedVelocity.x * 0.91,
                                (predictedVelocity.y - 0.08) * 0.98, predictedVelocity.z * 0.91);
                    }
                    position = latest.position();
                    velocity = closestVelocity;
                    sampleTick = latest.tick();
                }
                Vec3d beforeDrag = new Vec3d(velocity.x / 0.91, velocity.y / 0.98 + 0.08, velocity.z / 0.91);
                return new Result(position, beforeDrag, sampleTick, true, true, 0.0, 0.91);
            }
            if (!grounded) {
                history = recent;
            }
        }
        if (history.size() < 2) {
            Vec3d delta = player.getEntityPos().subtract(player.lastX, player.lastY, player.lastZ);
            boolean airborne = !player.isOnGround();
            int sampleTick = history.isEmpty() ? tick : history.get(history.size() - 1).tick();
            return new Result(player.getTrackedPosition().getPos(), airborne ? delta : delta.multiply(1.0, 0.0, 1.0),
                    sampleTick, airborne, false, 0.0, 1.0);
        }
        int count = history.size();
        PositionSample latest = history.get(count - 1);
        int start = count - 1;
        while (start > 0 && latest.tick() - history.get(start - 1).tick() <= 6) {
            start--;
        }
        for (int index = count - 1; index >= Math.max(1, start); index--) {
            if (history.get(index).position().equals(history.get(index - 1).position())) {
                if (index < count - 1) {
                    start = index + 1;
                }
                break;
            }
        }
        List<PositionSample> samples = history.subList(Math.min(start, count - 2), count);
        int firstTick = samples.get(0).tick();
        int elapsed = latest.tick() - firstTick;
        boolean reliable = samples.size() >= 3 && elapsed >= 3
                || samples.stream().allMatch(sample -> sample.position().equals(latest.position()));
        LinearFit fitX = fitAxis(samples, firstTick, Vec3d::getX);
        LinearFit fitY = fitAxis(samples, firstTick, Vec3d::getY);
        LinearFit fitZ = fitAxis(samples, firstTick, Vec3d::getZ);
        Vec3d velocity = new Vec3d(fitX.slope(), fitY.slope(), fitZ.slope());
        Vec3d position = new Vec3d(fitX.intercept(), fitY.intercept(), fitZ.intercept()).add(velocity.multiply(elapsed));
        double yawRate = 0.0;
        if (samples.size() >= 4) {
            int half = samples.size() / 2;
            List<PositionSample> early = samples.subList(0, half);
            List<PositionSample> late = samples.subList(samples.size() - half, samples.size());
            Vec3d earlyVelocity = horizontalVelocity(early);
            Vec3d lateVelocity = horizontalVelocity(late);
            double earlyTick = early.stream().mapToInt(PositionSample::tick).average().orElse(0.0);
            double lateTick = late.stream().mapToInt(PositionSample::tick).average().orElse(0.0);
            if (earlyVelocity.length() > 0.3 && lateVelocity.length() > 0.3 && lateTick > earlyTick) {
                double lateHeading = Math.atan2(lateVelocity.z, lateVelocity.x);
                double rate = MathHelper.wrapDegrees(Math.toDegrees(lateHeading - Math.atan2(earlyVelocity.z, earlyVelocity.x)))
                        * 0.01745329238474369 / (lateTick - earlyTick);
                if (Math.abs(rate) > 0.015) {
                    yawRate = rate;
                    double heading = lateHeading + rate * (latest.tick() - lateTick);
                    double speed = lateVelocity.length();
                    velocity = new Vec3d(Math.cos(heading) * speed, velocity.y, Math.sin(heading) * speed);
                    position = new Vec3d(latest.position().x, position.y, latest.position().z);
                }
            }
        }
        double heightChange = latest.position().y - samples.get(0).position().y;
        boolean stationaryHeight = Math.abs(heightChange) < 0.01 * Math.max(1, elapsed);
        if (latest.onGround() && stationaryHeight) {
            return new Result(new Vec3d(position.x, latest.position().y, position.z), velocity.multiply(1.0, 0.0, 1.0),
                    latest.tick(), false, reliable, yawRate, 1.0);
        }
        LinearFit falling = fitFallingHeight(samples, firstTick);
        boolean useGravity = samples.size() >= 3 ? falling.meanSquaredError() <= fitY.meanSquaredError()
                : !stationaryHeight && !latest.onGround();
        if (!useGravity) {
            return new Result(position, velocity, latest.tick(), false, reliable, yawRate, 1.0);
        }
        double verticalVelocity = falling.slope();
        double height = falling.intercept();
        for (int step = 0; step < elapsed; step++) {
            verticalVelocity = (verticalVelocity - 0.08) * 0.98;
            height += verticalVelocity;
        }
        return new Result(new Vec3d(position.x, height, position.z), new Vec3d(velocity.x, verticalVelocity, velocity.z),
                latest.tick(), true, true, yawRate, 1.0);
    }

    private static Vec3d horizontalVelocity(List<PositionSample> samples) {
        int tick = samples.get(0).tick();
        return new Vec3d(fitAxis(samples, tick, Vec3d::getX).slope(), 0.0, fitAxis(samples, tick, Vec3d::getZ).slope());
    }

    private static LinearFit fitAxis(List<PositionSample> samples, int firstTick, ToDoubleFunction<Vec3d> axis) {
        double[] times = new double[samples.size()];
        double[] values = new double[samples.size()];
        for (int index = 0; index < samples.size(); index++) {
            times[index] = samples.get(index).tick() - firstTick;
            values[index] = axis.applyAsDouble(samples.get(index).position());
        }
        return MotionRegression.fit(times, values);
    }

    private static LinearFit fitFallingHeight(List<PositionSample> samples, int firstTick) {
        double[] times = new double[samples.size()];
        double[] heights = new double[samples.size()];
        for (int index = 0; index < samples.size(); index++) {
            double drag = 1.0;
            double distanceSum = 0.0;
            double gravitySum = 0.0;
            for (int step = samples.get(index).tick() - firstTick; step > 0; step--) {
                drag *= 0.98;
                distanceSum += drag;
                gravitySum += distanceSum;
            }
            times[index] = distanceSum;
            heights[index] = samples.get(index).position().y + 0.08 * gravitySum;
        }
        return MotionRegression.fit(times, heights);
    }

    public static Box[] predictBoxes(PlayerEntity player, Result motion, double age, int ticks) {
        EntityDimensions dimensions = player.getDimensions(player.getPose());
        int steps = (int) Math.ceil(Math.max(0.0, age) + ticks) + 1;
        Vec3d[] positions = new Vec3d[steps + 1];
        Vec3d position = motion.position();
        Vec3d velocity = motion.velocity();
        boolean airborne = motion.airborne();
        positions[0] = position;
        for (int step = 1; step <= steps; step++) {
            if (airborne) {
                velocity = new Vec3d(velocity.x * motion.horizontalDrag(), (velocity.y - 0.08) * 0.98,
                        velocity.z * motion.horizontalDrag());
            }
            if (motion.yawRate() != 0.0) {
                velocity = velocity.rotateY((float) -motion.yawRate());
            }
            Vec3d next = position.add(velocity);
            if (!CLIENT.world.isSpaceEmpty(player, dimensions.getBoxAt(next))) {
                Vec3d horizontal = position.add(velocity.x, 0.0, velocity.z);
                if (CLIENT.world.isSpaceEmpty(player, dimensions.getBoxAt(horizontal))) {
                    next = horizontal;
                    velocity = velocity.multiply(1.0, 0.0, 1.0);
                    airborne = false;
                } else {
                    velocity = velocity.multiply(0.0, 1.0, 0.0);
                    next = position.add(velocity);
                    if (!CLIENT.world.isSpaceEmpty(player, dimensions.getBoxAt(next))) {
                        next = position;
                        velocity = Vec3d.ZERO;
                        airborne = false;
                    }
                }
            }
            positions[step] = next;
            position = next;
        }
        Box[] result = new Box[ticks + 1];
        for (int step = 0; step <= ticks; step++) {
            double time = Math.max(0.0, age) + step;
            int index = Math.min(steps - 1, (int) time);
            result[step] = dimensions.getBoxAt(positions[index].lerp(positions[index + 1], time - index));
        }
        return result;
    }

    public static MovingTargetLaunch findLaunch(TargetPath path) {
        int horizon = path.targetBoxes().length - 1;
        List<Rotation> angles = new ArrayList<>();
        double drag = 1.0;
        double distanceSum = 0.0;
        double gravitySum = 0.0;
        Vec3d previousVelocity = null;
        double previousError = 0.0;
        Vec3d closestVelocity = null;
        double closestError = Double.MAX_VALUE;
        for (int tick = 1; tick <= horizon; tick++) {
            drag *= 0.99;
            distanceSum += drag;
            gravitySum += distanceSum;
            Vec3d velocity = path.targetBoxes()[tick].getCenter().subtract(path.launchPosition())
                    .add(0.0, 0.03 * gravitySum, 0.0).multiply(1.0 / distanceSum).subtract(path.inheritedVelocity());
            double error = velocity.length() - 1.5;
            if (previousVelocity != null && Math.signum(error) != Math.signum(previousError)) {
                addAngle(angles, previousVelocity.lerp(velocity, previousError / (previousError - error)));
            }
            if (Math.abs(error) < closestError) {
                closestVelocity = velocity;
                closestError = Math.abs(error);
            }
            previousVelocity = velocity;
            previousError = error;
        }
        if (angles.isEmpty() && closestVelocity != null) {
            addAngle(angles, closestVelocity);
        }
        MovingTargetLaunch best = null;
        for (Rotation angle : angles) {
            MovingTargetLaunch candidate = refine(path, angle);
            if (candidate != null && (best == null || candidate.hitFraction() > best.hitFraction())) {
                best = candidate;
            }
        }
        return best;
    }

    private static void addAngle(List<Rotation> angles, Vec3d velocity) {
        if (velocity.lengthSquared() < 1.0E-8) {
            return;
        }
        Rotation angle = Rotation.lookingAt(Vec3d.ZERO, velocity);
        for (Rotation existing : angles) {
            if (Math.abs(existing.yawDifferenceTo(angle)) < 0.3F && Math.abs(existing.pitch() - angle.pitch()) < 0.3F) {
                return;
            }
        }
        angles.add(angle);
    }

    private static MovingTargetLaunch refine(TargetPath path, Rotation angle) {
        MovingTargetLaunch best = null;
        for (float yawOffset : ANGLE_OFFSETS) {
            for (float pitchOffset : ANGLE_OFFSETS) {
                MovingTargetLaunch candidate = evaluate(path, angle.yaw() + yawOffset,
                        MathHelper.clamp(angle.pitch() + pitchOffset, -90.0F, 90.0F));
                if (candidate != null && (best == null || candidate.hitFraction() > best.hitFraction())) {
                    best = candidate;
                }
                if (best != null && best.hitFraction() >= 1.0) {
                    return best;
                }
            }
        }
        return best;
    }

    public static double hitFraction(TargetPath path, float yaw, float pitch) {
        MovingTargetLaunch launch = evaluate(path, yaw, pitch);
        return launch == null ? 0.0 : launch.hitFraction();
    }

    private static MovingTargetLaunch evaluate(TargetPath path, float yaw, float pitch) {
        Vec3d direction = Vec3d.fromPolar(pitch, yaw);
        int horizon = path.targetBoxes().length - 1;
        int central = trace(path, direction, SPREAD[0], horizon, horizon, true);
        int spreadHorizon = central < 0 ? -central - 1 : horizon;
        int hits = central > 0 ? 1 : 0;
        int tickSum = Math.max(0, central);
        for (int sample = 1; sample < 16; sample++) {
            int hit = trace(path, direction, SPREAD[sample], spreadHorizon, 8, false);
            if (hit > 0) {
                hits++;
                tickSum += hit;
            }
        }
        return hits == 0 ? null : new MovingTargetLaunch(yaw, pitch, hits / 16.0, Math.round((float) tickSum / (float) hits));
    }

    private static int trace(TargetPath path, Vec3d direction, Vec3d spread, int horizon,
            int worldCollisionTicks, boolean checkObstructions) {
        Vec3d position = path.launchPosition();
        Vec3d velocity = direction.add(spread).multiply(1.5).add(path.inheritedVelocity());
        for (int tick = 1; tick <= horizon; tick++) {
            velocity = velocity.subtract(0.0, 0.03, 0.0).multiply(0.99);
            Vec3d next = position.add(velocity);
            if (path.targetBoxes()[tick].expand(TrajectoryTolerance.collisionExpansion(tick)).raycast(position, next).isPresent()) {
                return tick;
            }
            if (tick <= worldCollisionTicks && CLIENT.world.raycast(new RaycastContext(position, next,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, CLIENT.player))
                    .getType() != HitResult.Type.MISS) {
                return -tick;
            }
            if (checkObstructions && tick > 1) {
                for (Box obstruction : path.obstructionBoxes()) {
                    if (obstruction.expand(TrajectoryTolerance.collisionExpansion(tick)).raycast(position, next).isPresent()) {
                        return -tick;
                    }
                }
            }
            position = next;
        }
        return 0;
    }
}
