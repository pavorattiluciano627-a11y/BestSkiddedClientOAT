package dev.echo.client.utils.items;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;

/** Hand-swing operation used by the client item utilities. */
public final class ActiveHand {
    private ActiveHand() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /** Delegates to the entity, preserving its animation, packets, and exceptions. */
    public static void swingHand(LivingEntity entity, Hand hand) {
        entity.swingHand(hand);
    }
}
