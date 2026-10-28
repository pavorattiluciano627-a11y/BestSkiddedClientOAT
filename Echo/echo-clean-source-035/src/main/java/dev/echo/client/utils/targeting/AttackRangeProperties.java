package dev.echo.client.utils.targeting;

import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

/** Direct adapters to Minecraft's attack-range component. */
public final class AttackRangeProperties {
    private AttackRangeProperties() {
    }

    public static AttackRangeComponent getAttackRange(LivingEntity entity, ItemStack stack) {
        // The original API accepts a stack but reads only the entity's effective range.
        return entity.getAttackRange();
    }

    public static double getEffectiveMaxRange(PlayerEntity player) {
        return player.getAttackRange().getEffectiveMaxRange(player);
    }

    public static boolean isWithinRange(PlayerEntity player, Vec3d point) {
        if (player == null || point == null) {
            return false;
        }
        return player.getAttackRange().isWithinRange(player, point);
    }
}
