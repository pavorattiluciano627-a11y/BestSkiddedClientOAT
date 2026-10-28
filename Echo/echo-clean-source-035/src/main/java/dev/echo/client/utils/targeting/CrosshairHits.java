package dev.echo.client.utils.targeting;

import static dev.echo.client.MinecraftAccess.CLIENT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
        return createMiss(CLIENT.player.getEyePosition(), CLIENT.player.getDirection());
    }

    public static HitResult createMiss(Vec3 position, Direction side) {
        if (position == null || side == null) {
            return null;
        }
        return BlockHitResult.miss(position, side, BlockPos.containing(position));
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

    public static Vec3 getPosition(HitResult hit) {
        return hit == null ? null : hit.getLocation();
    }

    public static Direction getSide(HitResult hit) {
        if (!isBlock(hit)) {
            return null;
        }
        return ((BlockHitResult) hit).getDirection();
    }

    /** Replaces only an entity hit and returns the previous crosshair object. */
    public static HitResult retargetForItemUse() {
        if (CLIENT.hitResult == null || !isEntity(CLIENT.hitResult)) {
            return CLIENT.hitResult;
        }
        HitResult previous = CLIENT.hitResult;
        HitResult replacement = createMissFromPlayer();
        if (replacement != null) {
            CLIENT.hitResult = replacement;
        }
        return previous;
    }

    public static void setCrosshairTarget(HitResult hit) {
        CLIENT.hitResult = hit;
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
