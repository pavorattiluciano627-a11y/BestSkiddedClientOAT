package dev.echo.client.utils;

import net.minecraft.entity.effect.StatusEffect;

public final class StatusEffectProperties {
    private StatusEffectProperties() {
    }

    public static boolean isInstant(StatusEffect effect) {
        return effect.isInstant();
    }
}
