package dev.echo.client.utils.targeting;

import static dev.echo.client.MinecraftAccess.CLIENT;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Crosshair hit extraction and the original entity-hit suppression scope for item use.
 * Direct/cached binding and original key/control applicability remain separately
 * documented; all positions, sides, hit objects and actions remain live inputs.
 */
public final class CrosshairHits {
    public CrosshairHits() {}

    /** Creates the original missed-block result at the current player's eyes. */
    public static HitResult createMissFromPlayer() {
        if (CLIENT.player == null) {
            return null;
        }
        return createMiss(CLIENT.player.getEyePos(), CLIENT.player.getHorizontalFacing());
    }

    public static HitResult createMiss(Vec3d position, Direction side) {
        if (position == null || side == null) {
            return null;
        }
        return BlockHitResult.createMissed(position, side, BlockPos.ofFloored(position));
    }

    public static boolean isBlock(HitResult hit) {
        return hit != null && hit.getType() == HitResult.Type.BLOCK;
    }

    public static boolean isEntity(HitResult hit) {
        return hit != null && hit.getType() == HitResult.Type.ENTITY;
    }

    public static Entity getEntity(HitResult hit) {
        if (!isEntity(hit)) {
            return null;
        }
        return ((EntityHitResult) hit).getEntity();
    }

    public static BlockPos getBlockPos(HitResult hit) {
        if (!isBlock(hit)) {
            return null;
        }
        return ((BlockHitResult) hit).getBlockPos();
    }

    public static Vec3d getPosition(HitResult hit) {
        return hit == null ? null : hit.getPos();
    }

    public static Direction getSide(HitResult hit) {
        if (!isBlock(hit)) {
            return null;
        }
        return ((BlockHitResult) hit).getSide();
    }

    /** Replaces only an entity hit and returns the previous crosshair object. */
    public static HitResult retargetForItemUse() {
        if (CLIENT.crosshairTarget == null || !isEntity(CLIENT.crosshairTarget)) {
            return CLIENT.crosshairTarget;
        }
        HitResult previous = CLIENT.crosshairTarget;
        HitResult replacement = createMissFromPlayer();
        if (replacement != null) {
            CLIENT.crosshairTarget = replacement;
        }
        return previous;
    }

    public static void setCrosshairTarget(HitResult hit) {
        CLIENT.crosshairTarget = hit;
    }

    /**
     * Runs with the temporary item-use hit, then restores the saved object.
     * Setup is outside the try block. A restoration failure replaces an action
     * failure, following ordinary finally semantics without suppressed exceptions.
     */
    public static void withItemUseHit(Runnable action) {
        HitResult previous = retargetForItemUse();
        try {
            action.run();
        } finally {
            setCrosshairTarget(previous);
        }
    }
}
