package dev.echo.client.events.interaction;

import dev.echo.client.events.Event;
import dev.echo.client.events.input.KeybindingEvent;
import dev.echo.client.utils.targeting.AttackEvaluation;
import java.lang.invoke.MethodHandles;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

/**
 * An item-usage tick and its actual kinetic-attack evaluations.
 * Original NodeFactory identity. This analysis candidate retains the shared input
 * sentinel branch; key/cache and provider applicability are tracked in its receipt.
 */
public class ItemUsageTickEvent extends Event {
    private final LivingEntity user;
    private final ItemStack stack;
    private final int remainingUseTicks;
    private final List<AttackEvaluation> attackEvaluations;

    /**
     * Captures the original arguments, retaining references and unrestricted ticks.
     * @param user entity using the item, possibly null
     * @param stack item being used, possibly null
     * @param remainingUseTicks remaining ticks supplied by the producer
     * @param attackEvaluations original list, possibly null; it is not copied
     */
    public ItemUsageTickEvent(LivingEntity user, ItemStack stack, int remainingUseTicks,
                              List<AttackEvaluation> attackEvaluations) {
        super();
        this.user = user;
        this.stack = stack;
        this.remainingUseTicks = remainingUseTicks;
        this.attackEvaluations = attackEvaluations;
    }

    /**
     * Queries the current maximum use time and subtracts the remaining ticks.
     * @return the original integer subtraction, without clamping or caching
     */
    public int getElapsedUseTicks() {
        initializePayloadType(ItemStack.class);
        ItemStack currentStack = stack;
        initializePayloadType(LivingEntity.class);
        LivingEntity currentUser = user;
        return currentStack.getMaxUseTime(currentUser) - remainingUseTicks;
    }

    /**
     * Tests the list through its original stream and availability predicate.
     * @return whether any evaluation is available
     */
    public boolean hasAvailableAttack() {
        return attackEvaluations.stream().anyMatch(AttackEvaluation::isAvailable);
    }

    /**
     * Tests damage using the current shared sentinel separately for each element.
     * @return whether any evaluation satisfies the original damage predicate
     */
    public boolean willDamage() {
        return attackEvaluations.stream().anyMatch(ItemUsageTickEvent::matchesDamage);
    }

    private static boolean matchesDamage(AttackEvaluation evaluation) {
        String[] sentinel = KeybindingEvent.getInputSentinel();
        if (sentinel == null && !evaluation.isAvailable()) {
            return false;
        }
        return evaluation.damage();
    }

    /**
     * Returns the original user after the field-linkage type initialization.
     * @return the captured user reference
     */
    public LivingEntity getUser() {
        initializePayloadType(LivingEntity.class);
        return user;
    }

    /**
     * Returns the original stack after the field-linkage type initialization.
     * @return the captured stack reference
     */
    public ItemStack getStack() {
        initializePayloadType(ItemStack.class);
        return stack;
    }

    /**
     * Returns the supplied tick count unchanged.
     * @return remaining use ticks
     */
    public int getRemainingUseTicks() {
        return remainingUseTicks;
    }

    /**
     * Returns the producer's actual list, including its identity and mutability.
     * @return the captured list reference, possibly null
     */
    public List<AttackEvaluation> getAttackEvaluations() {
        return attackEvaluations;
    }

    private static void initializePayloadType(Class<?> type) {
        try {
            // Ordinary JVM class initialization; no reflective member dispatch.
            MethodHandles.lookup().ensureInitialized(type);
        } catch (IllegalAccessException inaccessibleType) {
            // Both call sites name public Minecraft classes in the unnamed module.
            throw new IllegalAccessError(inaccessibleType.getMessage());
        }
    }
}
