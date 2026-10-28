package dev.echo.client.utils.targeting;

import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

/** Actual Minecraft attack-range queries, with conditional entity-guard applicability recorded separately. */
public final class AttackRange {
    private AttackRange() {}

    public static AttackRangeComponent getAttackRange(LivingEntity entity, ItemStack stack) {
        // The artifact does not read the stack argument.
        return entity.getAttackRange();
    }

    public static boolean canAttack(PlayerEntity player, Entity target) {
        if (player == null || target == null) {
            return false;
        }
        return player.canAttackEntityIn(target.getBoundingBox(), 0.0);
    }

    public static boolean isWithinRange(PlayerEntity player, Vec3d point) {
        if (player == null || point == null) {
            return false;
        }
        return player.getAttackRange().isWithinRange(player, point);
    }

    public static double getEffectiveMaxRange(PlayerEntity player) {
        return player.getAttackRange().getEffectiveMaxRange(player);
    }
}
