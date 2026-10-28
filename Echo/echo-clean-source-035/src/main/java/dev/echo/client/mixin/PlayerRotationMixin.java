package dev.echo.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.movement.RotationContext;
import dev.echo.client.events.movement.RotationEvent;
import dev.echo.client.features.impl.movement.KeepSprintFeature;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerRotationMixin {
    @ModifyExpressionValue(method = "aiStep", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F", ordinal = 0))
    private float modifyTickMovementAtGetYawAtOrdinal0(float yaw) {
        if ((Object) this != MinecraftAccess.CLIENT.player) {
            return yaw;
        }
        return RotationEvent.dispatch(RotationContext.HEAD, yaw,
            MinecraftAccess.CLIENT.player.getXRot()).getYaw();
    }

    @Redirect(method = {"causeExtraKnockback", "doSweepAttack"}, at = @At(
        value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"))
    private float redirectKnockbackTargetAtGetYaw(Player player) {
        return player != MinecraftAccess.CLIENT.player ? player.getYRot()
            : RotationEvent.dispatch(RotationContext.ATTACK, player.getYRot(), player.getXRot()).getYaw();
    }

    @Redirect(method = "causeExtraKnockback", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 redirectKnockbackTargetAtMultiply(Vec3 velocity, double x, double y, double z) {
        if (!Echo.eventsDisabled && (Object) this == MinecraftAccess.CLIENT.player
                && Echo.featureManager != null) {
            KeepSprintFeature keepSprint = (KeepSprintFeature) Echo.featureManager.findFeature(KeepSprintFeature.class);
            if (keepSprint != null && keepSprint.isEnabled()) {
                float multiplier = KeepSprintFeature.speedKeptVelocity.getValue();
                return velocity.multiply(multiplier, y, multiplier);
            }
        }
        return velocity.multiply(x, y, z);
    }

    @Redirect(method = "causeExtraKnockback", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"))
    private void redirectKnockbackTargetAtSetSprinting(Player player, boolean sprinting) {
        if (!Echo.eventsDisabled && Echo.featureManager != null) {
            KeepSprintFeature keepSprint = (KeepSprintFeature) Echo.featureManager.findFeature(KeepSprintFeature.class);
            // The original hook does not restrict this predicate to the local player.
            if (keepSprint != null && keepSprint.isEnabled() && keepSprint.shouldKeepSprinting()) {
                return;
            }
        }
        player.setSprinting(sprinting);
    }
}
