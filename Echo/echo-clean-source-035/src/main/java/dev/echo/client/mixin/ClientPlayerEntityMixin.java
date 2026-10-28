package dev.echo.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import dev.echo.client.Echo;
import dev.echo.client.events.interaction.CrosshairTargetEvent;
import dev.echo.client.events.interaction.HandSwingEvent;
import dev.echo.client.events.lifecycle.PlayerTickEvent;
import dev.echo.client.events.movement.MovementPacketEvent;
import dev.echo.client.events.movement.MovementSpeedEvent;
import dev.echo.client.handlers.PitchLimits;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayer {
    @Unique private MovementPacketEvent echo$movementPacket;

    public ClientPlayerEntityMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void beforeMovementPackets(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        float pitch = PitchLimits.clampDegrees(getXRot());
        MovementPacketEvent.Pre event = new MovementPacketEvent.Pre(
                getX(), getY(), getZ(), onGround(), getYRot(), pitch);
        event.post();
        if (event.cancelled) {
            echo$movementPacket = null;
            callback.cancel();
        } else {
            echo$movementPacket = event;
        }
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void afterMovementPackets(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        new MovementPacketEvent.Post(getX(), getY(), getZ(), onGround(), getYRot(), getXRot()).post();
        echo$movementPacket = null;
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"), require = 0)
    private double usePacketX(double original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        return echo$movementPacket != null ? echo$movementPacket.getX() : original;
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"), require = 0)
    private double usePacketY(double original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        return echo$movementPacket != null ? echo$movementPacket.getY() : original;
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"), require = 0)
    private double usePacketZ(double original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        return echo$movementPacket != null ? echo$movementPacket.getZ() : original;
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"), require = 0)
    private float usePacketYaw(float original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        return echo$movementPacket != null ? (float) echo$movementPacket.getYaw() : original;
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"), require = 0)
    private float usePacketPitch(float original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        float pitch = echo$movementPacket != null ? (float) echo$movementPacket.getPitch() : original;
        return PitchLimits.clampDegrees(pitch);
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"), require = 0)
    private boolean usePacketOnGround(boolean original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        return echo$movementPacket != null ? echo$movementPacket.isOnGround() : original;
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;position()Lnet/minecraft/world/phys/Vec3;"), require = 0)
    private Vec3 usePacketPosition(Vec3 original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        return echo$movementPacket != null
                ? new Vec3(echo$movementPacket.getX(), echo$movementPacket.getY(), echo$movementPacket.getZ())
                : original;
    }

    @Inject(method = "swing", at = @At("HEAD"), cancellable = true)
    private void beforeHandSwing(InteractionHand hand, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        HandSwingEvent event = new HandSwingEvent((LocalPlayer) (Object) this, hand);
        event.post();
        if (event.cancelled) {
            callback.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void beforePlayerTick(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        PlayerTickEvent event = new PlayerTickEvent();
        event.post();
        if (event.cancelled) {
            callback.cancel();
        }
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isSlowDueToUsingItem()Z"), require = 0)
    private boolean modifySprintBlocking(boolean original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        MovementSpeedEvent event = new MovementSpeedEvent(1.0F, original, false, false);
        event.post();
        return !event.cancelled && event.isBlockedFromSprinting();
    }

    @ModifyExpressionValue(method = "modifyInput", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;itemUseSpeedMultiplier()F"), require = 0)
    private float modifyActiveItemSpeed(float original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        MovementSpeedEvent event = new MovementSpeedEvent(original, false, false, false);
        event.post();
        return event.cancelled ? 1.0F : event.getActiveItemSpeedMultiplier();
    }

    @ModifyExpressionValue(method = "modifyInput", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isMovingSlowly()Z"), require = 0)
    private boolean modifySneakingSlowdown(boolean original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        MovementSpeedEvent event = new MovementSpeedEvent(1.0F, false, original, false);
        event.post();
        return !event.cancelled && event.shouldSlowDown();
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isPassenger()Z", ordinal = 1), require = 0)
    private boolean modifyMovementVehicleCheck(LocalPlayer player) {
        if (Echo.eventsDisabled) {
            return player.isPassenger();
        }
        if (player.isPassenger()) {
            return true;
        }
        MovementSpeedEvent event = new MovementSpeedEvent(1.0F, false, false, false);
        event.post();
        return event.hasVehicle();
    }

    @Redirect(method = "modifyInput", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isPassenger()Z"), require = 0)
    private boolean modifySpeedVehicleCheck(LocalPlayer player) {
        if (Echo.eventsDisabled) {
            return player.isPassenger();
        }
        if (player.isPassenger()) {
            return true;
        }
        MovementSpeedEvent event = new MovementSpeedEvent(1.0F, false, false, false);
        event.post();
        return event.hasVehicle();
    }

    @Inject(method = "raycastHitResult", at = @At("RETURN"), cancellable = true)
    private void updateCrosshairTarget(float tickProgress, Entity cameraEntity,
            CallbackInfoReturnable<HitResult> callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        CrosshairTargetEvent event = new CrosshairTargetEvent(callback.getReturnValue());
        event.post();
        if (event.target != callback.getReturnValue()) {
            callback.setReturnValue(event.target);
        }
    }
}
