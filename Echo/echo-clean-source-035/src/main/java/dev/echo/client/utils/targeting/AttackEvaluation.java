package dev.echo.client.utils.targeting;

import net.minecraft.entity.Entity;

/** Candidate kinetic contact result; boolean order follows the constructor call. */
public record AttackEvaluation(Entity target, boolean damage, boolean knockback,
        boolean dismount, float damageAmount, double attackerSpeed,
        double approachSpeed, boolean onCooldown) {
    public boolean isAvailable() {
        return !onCooldown;
    }
}
