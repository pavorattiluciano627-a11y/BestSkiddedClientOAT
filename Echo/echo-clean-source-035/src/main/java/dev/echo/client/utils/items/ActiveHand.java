package dev.echo.client.utils.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

/** Hand-swing operation used by the client item utilities. */
public final class ActiveHand {
    private ActiveHand() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /** Delegates to the entity, preserving its animation, packets, and exceptions. */
    public static void swingHand(LivingEntity entity, InteractionHand hand) {
        entity.swing(hand);
    }
}
