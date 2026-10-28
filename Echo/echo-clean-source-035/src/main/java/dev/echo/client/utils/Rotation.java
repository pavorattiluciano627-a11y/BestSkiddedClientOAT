package dev.echo.client.utils;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** A yaw/pitch pair in degrees. Pitch is restricted to the player's vertical range. */
public record Rotation(float yaw, float pitch) {
    public Rotation {
        pitch = Mth.clamp(pitch, -90.0F, 90.0F);
    }

    /** Returns the rotation looking from {@code origin} toward {@code target}. */
    public static Rotation lookingAt(Vec3 origin, Vec3 target) {
        Vec3 direction = target.subtract(origin);
        double horizontalDistance = Math.hypot(direction.x, direction.z);
        return new Rotation(
                Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F),
                Mth.wrapDegrees((float) -Math.toDegrees(Math.atan2(direction.y, horizontalDistance))));
    }

    /** Chooses this yaw's equivalent angle nearest the reference's wrapped yaw. */
    public Rotation relativeTo(Rotation reference) {
        float referenceYaw = Mth.wrapDegrees(reference.yaw);
        return new Rotation(referenceYaw + Mth.wrapDegrees(yaw - referenceYaw), pitch);
    }

    /** Returns the signed shortest yaw change from this rotation to the target. */
    public float yawDifferenceTo(Rotation target) {
        return Mth.wrapDegrees(target.yaw - yaw);
    }

    /** Moves each axis toward the target, with independent angular step limits. */
    public Rotation stepToward(Rotation target, float maxYawStep, float maxPitchStep) {
        float yawDifference = Mth.wrapDegrees(target.yaw - yaw);
        float nextYaw = yaw + Mth.clamp(yawDifference, -maxYawStep, maxYawStep);
        float pitchDifference = target.pitch - pitch;
        float nextPitch = pitch + Mth.clamp(pitchDifference, -maxPitchStep, maxPitchStep);
        return new Rotation(nextYaw, nextPitch);
    }
}
