package dev.echo.client.utils;

import net.minecraft.world.effect.MobEffect;

public final class StatusEffectProperties {
    private StatusEffectProperties() {
    }

    public static boolean isInstant(MobEffect effect) {
        return effect.isInstantaneous();
    }
}
