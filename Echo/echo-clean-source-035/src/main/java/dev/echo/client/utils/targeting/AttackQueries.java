package dev.echo.client.utils.targeting;

import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Item-use prediction bookkeeping shared by the original kinetic-weapon usage hook. */
public final class AttackQueries {
    private static final EntityQueryCache contactCache = new EntityQueryCache();
    private static int previousElapsedUseTicks = Integer.MAX_VALUE;

    private AttackQueries() {
    }

    /**
     * Predicts this usage tick with the shared contact cache. Equal or rewound
     * elapsed use time starts a new cache interval; no user/stack identity is
     * added to the original bookkeeping rule.
     */
    public static List<AttackEvaluation> findKineticWeaponTargets(LivingEntity user, ItemStack stack,
            int remainingUseTicks) {
        int elapsedUseTicks = stack.getUseDuration(user) - remainingUseTicks;
        if (elapsedUseTicks <= previousElapsedUseTicks) {
            contactCache.clear();
        }
        previousElapsedUseTicks = elapsedUseTicks;
        // The predictor deliberately queries maximum use time again after its
        // component lookup, preserving the original evaluation/exception order.
        return KineticAttackEvaluation.predict(user, stack, remainingUseTicks, contactCache);
    }
}
