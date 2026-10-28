package dev.echo.client.utils.targeting;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Actual Minecraft attack-range queries, with conditional entity-guard applicability recorded separately. */
public final class AttackRange {
    private AttackRange() {}

    public static net.minecraft.world.item.component.AttackRange getAttackRange(LivingEntity entity, ItemStack stack) {
        // The artifact does not read the stack argument.
        return entity.getAttackRangeWith(stack);
    }

    public static boolean canAttack(Player player, Entity target) {
        if (player == null || target == null) {
            return false;
        }
        return player.isWithinAttackRange(player.getMainHandItem(), target.getBoundingBox(), 0.0);
    }

    public static boolean isWithinRange(Player player, Vec3 point) {
        if (player == null || point == null) {
            return false;
        }
        return player.getAttackRangeWith(player.getMainHandItem()).isInRange(player, point);
    }

    public static double getEffectiveMaxRange(Player player) {
        return player.getAttackRangeWith(player.getMainHandItem()).effectiveMaxRange(player);
    }
}
