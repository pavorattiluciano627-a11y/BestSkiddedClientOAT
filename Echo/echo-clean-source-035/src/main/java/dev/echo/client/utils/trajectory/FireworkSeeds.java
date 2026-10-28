package dev.echo.client.utils.trajectory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;

/** Deterministic simulation seeds. These values are gameplay randomness, not linkage keys. */
public final class FireworkSeeds {
    private static final long SEED_BASE = -7046029254386353131L;
    private static final long ENTITY_MULTIPLIER = -4658895280553007687L;
    private static final long SHOT_MULTIPLIER = -7723592293110705685L;
    private static final long STACK_MULTIPLIER = -2960836687051489901L;

    private FireworkSeeds() {}

    public static long forShot(Player player, ItemStack stack, int shotIndex) {
        long seed = SEED_BASE;
        seed ^= player.getId() * ENTITY_MULTIPLIER;
        seed ^= shotIndex * SHOT_MULTIPLIER;
        seed ^= ItemStack.hashItemAndComponents(stack) * STACK_MULTIPLIER;
        return seed;
    }

    public static long forRocket(FireworkRocketEntity rocket) {
        long seed = SEED_BASE;
        seed ^= rocket.getId() * ENTITY_MULTIPLIER;
        seed ^= ItemStack.hashItemAndComponents(rocket.getItem()) * STACK_MULTIPLIER;
        return seed;
    }
}
