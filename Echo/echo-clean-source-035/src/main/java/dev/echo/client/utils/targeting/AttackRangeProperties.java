package dev.echo.client.utils.targeting;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.phys.Vec3;

/** Direct adapters to Minecraft's attack-range component. */
public final class AttackRangeProperties {
    private AttackRangeProperties() {
    }

    public static AttackRange getAttackRange(LivingEntity entity, ItemStack stack) {
        // The original API accepts a stack but reads only the entity's effective range.
        return entity.getAttackRangeWith(stack);
    }

    public static double getEffectiveMaxRange(Player player) {
        return player.getAttackRangeWith(player.getMainHandItem()).effectiveMaxRange(player);
    }

    public static boolean isWithinRange(Player player, Vec3 point) {
        if (player == null || point == null) {
            return false;
        }
        return player.getAttackRangeWith(player.getMainHandItem()).isInRange(player, point);
    }
}
