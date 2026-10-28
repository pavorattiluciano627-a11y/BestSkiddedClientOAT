package dev.echo.client.utils;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** A yaw/pitch pair in degrees. Pitch is restricted to the player's vertical range. */
public record Rotation(float yaw, float pitch) {
    public Rotation {
        pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
    }

    /** Returns the rotation looking from {@code origin} toward {@code target}. */
    public static Rotation lookingAt(Vec3d origin, Vec3d target) {
        Vec3d direction = target.subtract(origin);
        double horizontalDistance = Math.hypot(direction.x, direction.z);
        return new Rotation(
                MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F),
                MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(direction.y, horizontalDistance))));
    }

    /** Chooses this yaw's equivalent angle nearest the reference's wrapped yaw. */
    public Rotation relativeTo(Rotation reference) {
        float referenceYaw = MathHelper.wrapDegrees(reference.yaw);
        return new Rotation(referenceYaw + MathHelper.wrapDegrees(yaw - referenceYaw), pitch);
    }

    /** Returns the signed shortest yaw change from this rotation to the target. */
    public float yawDifferenceTo(Rotation target) {
        return MathHelper.wrapDegrees(target.yaw - yaw);
    }

    /** Moves each axis toward the target, with independent angular step limits. */
    public Rotation stepToward(Rotation target, float maxYawStep, float maxPitchStep) {
        float yawDifference = MathHelper.wrapDegrees(target.yaw - yaw);
        float nextYaw = yaw + MathHelper.clamp(yawDifference, -maxYawStep, maxYawStep);
        float pitchDifference = target.pitch - pitch;
        float nextPitch = pitch + MathHelper.clamp(pitchDifference, -maxPitchStep, maxPitchStep);
        return new Rotation(nextYaw, nextPitch);
    }
}
