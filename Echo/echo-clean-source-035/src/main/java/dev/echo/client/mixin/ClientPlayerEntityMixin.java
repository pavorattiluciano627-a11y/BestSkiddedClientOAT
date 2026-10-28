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
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
    @Unique private MovementPacketEvent echo$movementPacket;

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void beforeMovementPackets(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        float pitch = PitchLimits.clampDegrees(getPitch());
        MovementPacketEvent.Pre event = new MovementPacketEvent.Pre(
                getX(), getY(), getZ(), isOnGround(), getYaw(), pitch);
        event.post();
        if (event.cancelled) {
            echo$movementPacket = null;
            callback.cancel();
        } else {
            echo$movementPacket = event;
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("RETURN"))
    private void afterMovementPackets(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        new MovementPacketEvent.Post(getX(), getY(), getZ(), isOnGround(), getYaw(), getPitch()).post();
        echo$movementPacket = null;
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getX()D"))
    private double usePacketX(double original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        return echo$movementPacket != null ? echo$movementPacket.getX() : original;
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getY()D"))
    private double usePacketY(double original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        return echo$movementPacket != null ? echo$movementPacket.getY() : original;
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getZ()D"))
    private double usePacketZ(double original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        return echo$movementPacket != null ? echo$movementPacket.getZ() : original;
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float usePacketYaw(float original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        return echo$movementPacket != null ? (float) echo$movementPacket.getYaw() : original;
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float usePacketPitch(float original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        float pitch = echo$movementPacket != null ? (float) echo$movementPacket.getPitch() : original;
        return PitchLimits.clampDegrees(pitch);
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;isOnGround()Z"))
    private boolean usePacketOnGround(boolean original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        return echo$movementPacket != null ? echo$movementPacket.isOnGround() : original;
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getEntityPos()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d usePacketPosition(Vec3d original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        return echo$movementPacket != null
                ? new Vec3d(echo$movementPacket.getX(), echo$movementPacket.getY(), echo$movementPacket.getZ())
                : original;
    }

    @Inject(method = "swingHand", at = @At("HEAD"), cancellable = true)
    private void beforeHandSwing(Hand hand, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        HandSwingEvent event = new HandSwingEvent((ClientPlayerEntity) (Object) this, hand);
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

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;isBlockedFromSprinting()Z"))
    private boolean modifySprintBlocking(boolean original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        MovementSpeedEvent event = new MovementSpeedEvent(1.0F, original, false, false);
        event.post();
        return !event.cancelled && event.isBlockedFromSprinting();
    }

    @ModifyExpressionValue(method = "applyMovementSpeedFactors", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getActiveItemSpeedMultiplier()F"))
    private float modifyActiveItemSpeed(float original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        MovementSpeedEvent event = new MovementSpeedEvent(original, false, false, false);
        event.post();
        return event.cancelled ? 1.0F : event.getActiveItemSpeedMultiplier();
    }

    @ModifyExpressionValue(method = "applyMovementSpeedFactors", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;shouldSlowDown()Z"))
    private boolean modifySneakingSlowdown(boolean original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        MovementSpeedEvent event = new MovementSpeedEvent(1.0F, false, original, false);
        event.post();
        return !event.cancelled && event.shouldSlowDown();
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasVehicle()Z", ordinal = 1))
    private boolean modifyMovementVehicleCheck(ClientPlayerEntity player) {
        if (Echo.eventsDisabled) {
            return player.hasVehicle();
        }
        if (player.hasVehicle()) {
            return true;
        }
        MovementSpeedEvent event = new MovementSpeedEvent(1.0F, false, false, false);
        event.post();
        return event.hasVehicle();
    }

    @Redirect(method = "applyMovementSpeedFactors", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasVehicle()Z"))
    private boolean modifySpeedVehicleCheck(ClientPlayerEntity player) {
        if (Echo.eventsDisabled) {
            return player.hasVehicle();
        }
        if (player.hasVehicle()) {
            return true;
        }
        MovementSpeedEvent event = new MovementSpeedEvent(1.0F, false, false, false);
        event.post();
        return event.hasVehicle();
    }

    @Inject(method = "getCrosshairTarget", at = @At("RETURN"), cancellable = true)
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
