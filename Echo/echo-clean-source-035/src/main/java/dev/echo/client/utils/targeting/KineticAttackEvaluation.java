package dev.echo.client.utils.targeting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.component.type.KineticWeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

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
        KineticWeaponComponent weapon = stack.get(DataComponentTypes.KINETIC_WEAPON);
        if (weapon == null) {
            return List.of();
        }
        int activeTicks = stack.getMaxUseTime(attacker) - remainingUseTicks;
        if (activeTicks < weapon.delayTicks()) {
            return List.of();
        }
        activeTicks -= weapon.delayTicks();
        Vec3d direction = attacker.getRotationVec(1.0F);
        double attackerSpeed = direction.dotProduct(KineticWeaponComponent.getAmplifiedMovement(attacker));
        float speedScale = attacker instanceof PlayerEntity ? 1.0F : 0.2F;
        AttackRangeComponent range = AttackRangeProperties.getAttackRange(attacker, stack);
        return evaluateContacts(attacker, weapon, activeTicks, range, cache, direction, attackerSpeed, speedScale);
    }

    /** Retains the earlier explicit-component entry without applying item-use/reset bookkeeping. */
    public static List<AttackEvaluation> evaluateCollisions(LivingEntity attacker,
            KineticWeaponComponent weapon, int activeTicks, AttackRangeComponent range,
            EntityQueryCache cache) {
        Vec3d direction = attacker.getRotationVec(1.0F);
        double attackerSpeed = direction.dotProduct(KineticWeaponComponent.getAmplifiedMovement(attacker));
        float speedScale = attacker instanceof PlayerEntity ? 1.0F : 0.2F;
        return evaluateContacts(attacker, weapon, activeTicks, range, cache, direction, attackerSpeed, speedScale);
    }

    private static List<AttackEvaluation> evaluateContacts(LivingEntity attacker, KineticWeaponComponent weapon,
            int activeTicks, AttackRangeComponent range, EntityQueryCache cache,
            Vec3d direction, double attackerSpeed, float speedScale) {
        double attackDamage = attacker.getAttributeBaseValue(EntityAttributes.ATTACK_DAMAGE);
        long time = attacker.getEntityWorld().getTime();
        Collection<EntityHitResult> contacts = KineticCollisionQueries.collectHits(attacker, range);
        List<AttackEvaluation> results = new ArrayList<>();
        for (EntityHitResult contact : contacts) {
            Entity target = contact.getEntity();
            if (target instanceof EnderDragonPart part) {
                target = part.owner;
            }
            boolean onCooldown = cache != null
                    && cache.wasQueriedRecently(time, target, weapon.contactCooldownTicks());
            if (!onCooldown && cache != null) {
                // Contact is recorded before condition evaluation, even when no result is added.
                cache.recordQuery(time, target);
            }
            double targetSpeed = direction.dotProduct(KineticWeaponComponent.getAmplifiedMovement(target));
            double approachSpeed = Math.max(0.0, attackerSpeed - targetSpeed);
            boolean dismount = weapon.dismountConditions().isPresent()
                    && weapon.dismountConditions().get().isSatisfied(
                            activeTicks, attackerSpeed, approachSpeed, speedScale);
            boolean knockback = weapon.knockbackConditions().isPresent()
                    && weapon.knockbackConditions().get().isSatisfied(
                            activeTicks, attackerSpeed, approachSpeed, speedScale);
            boolean damage = weapon.damageConditions().isPresent()
                    && weapon.damageConditions().get().isSatisfied(
                            activeTicks, attackerSpeed, approachSpeed, speedScale);
            if (dismount || knockback || damage) {
                float damageAmount = (float) attackDamage
                        + (float) MathHelper.floor(approachSpeed * weapon.damageMultiplier());
                results.add(new AttackEvaluation(target, damage, knockback, dismount,
                        damageAmount, attackerSpeed, approachSpeed, onCooldown));
            }
        }
        return results;
    }
}
