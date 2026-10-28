package dev.echo.client.utils.targeting;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.mixin.ClientPlayerInteractionManagerAccessor;
import dev.echo.client.mixin.LivingEntityAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;

/**
 * Prospective main-hand attack timing and the client's actual piercing action.
 * Original key/control, mixin and shared-cache applicability are recorded in the
 * provider receipt. Player age, equipment history and attributes remain live.
 */
public final class WeaponAttributes implements MinecraftAccess {
    private static int lastSwingTick = -1;

    private WeaponAttributes() {}

    /** Records the original shared swing-age value; null leaves the prior value. */
    public static void recordSwingTick(Player player) {
        if (player == null) {
            return;
        }
        lastSwingTick = player.tickCount;
    }

    /** Remaining cooldown for the supplied prospective stack, including fractional ticks. */
    public static double remainingCooldownSeconds(Player player, ItemStack stack, float tickDelta) {
        float delayTicks = projectedAttackDelayTicks(player, stack);
        if (delayTicks <= 0.0F) {
            return 0.0;
        }
        // Both operations are float operations before the division widens to double.
        float remainingTicks = delayTicks - (elapsedCooldownTicks(player) + tickDelta);
        return Math.max(0.0, remainingTicks / 20.0);
    }

    private static int elapsedCooldownTicks(Player player) {
        if (player == null) {
            return 0;
        }
        int ticksSinceEquipping = ((LivingEntityAccessor) player).getTicksSinceHandEquipping();
        if (lastSwingTick < 0 || lastSwingTick > player.tickCount) {
            return ticksSinceEquipping;
        }
        return Math.min(ticksSinceEquipping, player.tickCount - lastSwingTick);
    }

    private static float projectedAttackDelayTicks(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return 0.0F;
        }
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return player.getCurrentItemAttackStrengthDelay();
        }
        double[] additive = {0.0};
        double[] baseMultiplier = {0.0};
        double[] totalMultiplier = {1.0};
        for (AttributeModifier modifier : attackSpeed.getModifiers()) {
            if (modifier.is(Item.BASE_ATTACK_SPEED_ID)) {
                continue;
            }
            accumulateModifier(modifier, additive, baseMultiplier, totalMultiplier);
        }
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_SPEED)) {
                accumulateModifier(modifier, additive, baseMultiplier, totalMultiplier);
            }
        });
        double projectedSpeed = (attackSpeed.getBaseValue() + additive[0])
                * (1.0 + baseMultiplier[0]) * totalMultiplier[0];
        projectedSpeed = Attributes.ATTACK_SPEED.value().sanitizeValue(projectedSpeed);
        if (projectedSpeed <= 0.0) {
            return 0.0F;
        }
        return (float) (20.0 / projectedSpeed);
    }

    private static void accumulateModifier(AttributeModifier modifier, double[] additive,
                                           double[] baseMultiplier, double[] totalMultiplier) {
        switch (modifier.operation()) {
            case ADD_VALUE -> additive[0] += modifier.amount();
            case ADD_MULTIPLIED_BASE -> baseMultiplier[0] += modifier.amount();
            case ADD_MULTIPLIED_TOTAL -> totalMultiplier[0] *= 1.0 + modifier.amount();
        }
    }

    /** Invokes the vanilla piercing interaction, then swings the current main hand. */
    public static void performPiercingAttack() {
        if (CLIENT.player == null || CLIENT.gameMode == null) {
            return;
        }
        PiercingWeapon weapon = CLIENT.player.getMainHandItem().get(DataComponents.PIERCING_WEAPON);
        if (weapon == null) {
            return;
        }
        ((ClientPlayerInteractionManagerAccessor) CLIENT.gameMode).invokeAttackWithPiercingWeapon(weapon);
        CLIENT.player.swing(InteractionHand.MAIN_HAND);
    }
}
