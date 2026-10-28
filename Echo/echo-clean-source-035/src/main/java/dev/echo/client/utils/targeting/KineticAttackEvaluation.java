package dev.echo.client.utils.targeting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Complete kinetic-weapon prediction and its reusable collision-evaluation
 * operation. Original key/cache and null targeting-sentinel applicability remain
 * recorded conditions; semantic entities, stacks and use times are unrestricted.
 */
public final class KineticAttackEvaluation {
    private KineticAttackEvaluation() {
    }

    /** Predicts without changing the global use-time/reset bookkeeping. A null cache is allowed. */
    public static List<AttackEvaluation> predict(LivingEntity attacker, ItemStack stack,
            int remainingUseTicks, EntityQueryCache cache) {
        KineticWeapon weapon = stack.get(DataComponents.KINETIC_WEAPON);
        if (weapon == null) {
            return List.of();
        }
        int activeTicks = stack.getUseDuration(attacker) - remainingUseTicks;
        if (activeTicks < weapon.delayTicks()) {
            return List.of();
        }
        activeTicks -= weapon.delayTicks();
        Vec3 direction = attacker.getViewVector(1.0F);
        double attackerSpeed = direction.dot(KineticWeapon.getMotion(attacker));
        float speedScale = attacker instanceof Player ? 1.0F : 0.2F;
        AttackRange range = AttackRangeProperties.getAttackRange(attacker, stack);
        return evaluateContacts(attacker, weapon, activeTicks, range, cache, direction, attackerSpeed, speedScale);
    }

    /** Retains the earlier explicit-component entry without applying item-use/reset bookkeeping. */
    public static List<AttackEvaluation> evaluateCollisions(LivingEntity attacker,
            KineticWeapon weapon, int activeTicks, AttackRange range,
            EntityQueryCache cache) {
        Vec3 direction = attacker.getViewVector(1.0F);
        double attackerSpeed = direction.dot(KineticWeapon.getMotion(attacker));
        float speedScale = attacker instanceof Player ? 1.0F : 0.2F;
        return evaluateContacts(attacker, weapon, activeTicks, range, cache, direction, attackerSpeed, speedScale);
    }

    private static List<AttackEvaluation> evaluateContacts(LivingEntity attacker, KineticWeapon weapon,
            int activeTicks, AttackRange range, EntityQueryCache cache,
            Vec3 direction, double attackerSpeed, float speedScale) {
        double attackDamage = attacker.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        long time = attacker.level().getGameTime();
        Collection<EntityHitResult> contacts = KineticCollisionQueries.collectHits(attacker, range);
        List<AttackEvaluation> results = new ArrayList<>();
        for (EntityHitResult contact : contacts) {
            Entity target = contact.getEntity();
            if (target instanceof EnderDragonPart part) {
                target = part.parentMob;
            }
            boolean onCooldown = cache != null
                    && cache.wasQueriedRecently(time, target, weapon.contactCooldownTicks());
            if (!onCooldown && cache != null) {
                // Contact is recorded before condition evaluation, even when no result is added.
                cache.recordQuery(time, target);
            }
            double targetSpeed = direction.dot(KineticWeapon.getMotion(target));
            double approachSpeed = Math.max(0.0, attackerSpeed - targetSpeed);
            boolean dismount = weapon.dismountConditions().isPresent()
                    && weapon.dismountConditions().get().test(
                            activeTicks, attackerSpeed, approachSpeed, speedScale);
            boolean knockback = weapon.knockbackConditions().isPresent()
                    && weapon.knockbackConditions().get().test(
                            activeTicks, attackerSpeed, approachSpeed, speedScale);
            boolean damage = weapon.damageConditions().isPresent()
                    && weapon.damageConditions().get().test(
                            activeTicks, attackerSpeed, approachSpeed, speedScale);
            if (dismount || knockback || damage) {
                float damageAmount = (float) attackDamage
                        + (float) Mth.floor(approachSpeed * weapon.damageMultiplier());
                results.add(new AttackEvaluation(target, damage, knockback, dismount,
                        damageAmount, attackerSpeed, approachSpeed, onCooldown));
            }
        }
        return results;
    }
}
