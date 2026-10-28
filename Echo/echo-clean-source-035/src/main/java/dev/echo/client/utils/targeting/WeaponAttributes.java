package dev.echo.client.utils.targeting;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.mixin.ClientPlayerInteractionManagerAccessor;
import dev.echo.client.mixin.LivingEntityAccessor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

/**
 * Prospective main-hand attack timing and the client's actual piercing action.
 * Original key/control, mixin and shared-cache applicability are recorded in the
 * provider receipt. Player age, equipment history and attributes remain live.
 */
public final class WeaponAttributes implements MinecraftAccess {
    private static int lastSwingTick = -1;

    private WeaponAttributes() {}

    /** Records the original shared swing-age value; null leaves the prior value. */
    public static void recordSwingTick(PlayerEntity player) {
        if (player == null) {
            return;
        }
        lastSwingTick = player.age;
    }

    /** Remaining cooldown for the supplied prospective stack, including fractional ticks. */
    public static double remainingCooldownSeconds(PlayerEntity player, ItemStack stack, float tickDelta) {
        float delayTicks = projectedAttackDelayTicks(player, stack);
        if (delayTicks <= 0.0F) {
            return 0.0;
        }
        // Both operations are float operations before the division widens to double.
        float remainingTicks = delayTicks - (elapsedCooldownTicks(player) + tickDelta);
        return Math.max(0.0, remainingTicks / 20.0);
    }

    private static int elapsedCooldownTicks(PlayerEntity player) {
        if (player == null) {
            return 0;
        }
        int ticksSinceEquipping = ((LivingEntityAccessor) player).getTicksSinceHandEquipping();
        if (lastSwingTick < 0 || lastSwingTick > player.age) {
            return ticksSinceEquipping;
        }
        return Math.min(ticksSinceEquipping, player.age - lastSwingTick);
    }

    private static float projectedAttackDelayTicks(PlayerEntity player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return 0.0F;
        }
        EntityAttributeInstance attackSpeed = player.getAttributeInstance(EntityAttributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return player.getAttackCooldownProgressPerTick();
        }
        double[] additive = {0.0};
        double[] baseMultiplier = {0.0};
        double[] totalMultiplier = {1.0};
        for (EntityAttributeModifier modifier : attackSpeed.getModifiers()) {
            if (modifier.idMatches(Item.BASE_ATTACK_SPEED_MODIFIER_ID)) {
                continue;
            }
            accumulateModifier(modifier, additive, baseMultiplier, totalMultiplier);
        }
        stack.applyAttributeModifiers(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(EntityAttributes.ATTACK_SPEED)) {
                accumulateModifier(modifier, additive, baseMultiplier, totalMultiplier);
            }
        });
        double projectedSpeed = (attackSpeed.getBaseValue() + additive[0])
                * (1.0 + baseMultiplier[0]) * totalMultiplier[0];
        projectedSpeed = EntityAttributes.ATTACK_SPEED.value().clamp(projectedSpeed);
        if (projectedSpeed <= 0.0) {
            return 0.0F;
        }
        return (float) (20.0 / projectedSpeed);
    }

    private static void accumulateModifier(EntityAttributeModifier modifier, double[] additive,
                                           double[] baseMultiplier, double[] totalMultiplier) {
        switch (modifier.operation()) {
            case ADD_VALUE -> additive[0] += modifier.value();
            case ADD_MULTIPLIED_BASE -> baseMultiplier[0] += modifier.value();
            case ADD_MULTIPLIED_TOTAL -> totalMultiplier[0] *= 1.0 + modifier.value();
        }
    }

    /** Invokes the vanilla piercing interaction, then swings the current main hand. */
    public static void performPiercingAttack() {
        if (CLIENT.player == null || CLIENT.interactionManager == null) {
            return;
        }
        PiercingWeaponComponent weapon = CLIENT.player.getMainHandStack().get(DataComponentTypes.PIERCING_WEAPON);
        if (weapon == null) {
            return;
        }
        ((ClientPlayerInteractionManagerAccessor) CLIENT.interactionManager).invokeAttackWithPiercingWeapon(weapon);
        CLIENT.player.swingHand(Hand.MAIN_HAND);
    }
}
