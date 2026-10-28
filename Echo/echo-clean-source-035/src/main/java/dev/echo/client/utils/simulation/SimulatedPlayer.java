package dev.echo.client.utils.simulation;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Complete functional simulation candidate for the documented normal control domain.
 * Dispatch/cache and opaque-state obligations are retained in the continuation ledger.
 */
public class SimulatedPlayer extends SimulationPlayerBase {
    private static final GameProfile SIMULATION_PROFILE = new GameProfile(
            UUID.nameUUIDFromBytes("echo:simulated-player".getBytes(StandardCharsets.UTF_8)), "echo_sim");
    private Player sourcePlayer;
    private int jumpCooldown;

    public SimulatedPlayer(ClientLevel world) {
        super(world, SIMULATION_PROFILE);
        noPhysics = false;
        firstTick = false;
        setInvisible(true);
        setSilent(true);
    }

    public void copyFrom(Player player, Vec3 velocity, float yaw, float pitch,
            float forwardInput, float sidewaysInput, boolean jumpPressed) {
        sourcePlayer = player;
        getAbilities().apply(player.getAbilities().pack());
        setSprinting(player.isSprinting());
        getAttributes().assignAllValues(player.getAttributes());
        setSpeed(player.getSpeed());
        setPose(player.getPose());
        refreshDimensions();
        setPos(player.getX(), player.getY(), player.getZ());
        setOldPosAndRot();
        setDeltaMovement(velocity);
        setOnGround(player.onGround());
        horizontalCollision = player.horizontalCollision;
        verticalCollision = player.verticalCollision;
        verticalCollisionBelow = player.verticalCollisionBelow;
        minorHorizontalCollision = player.minorHorizontalCollision;
        fallDistance = player.fallDistance;
        stuckSpeedMultiplier = Vec3.ZERO;
        setYRot(yaw);
        setXRot(pitch);
        setYHeadRot(yaw);
        setYBodyRot(yaw);
        setShiftKeyDown(player.isShiftKeyDown());
        setSwimming(player.isSwimming());
        // The original Entity invoker forwards exactly to this protected setter.
        setSharedFlag(7, player.isFallFlying());
        xxa = sidewaysInput;
        yya = 0.0F;
        zza = forwardInput;
        jumping = jumpPressed;
        jumpCooldown = 0;
    }

    public void tickPrediction() {
        setOldPosAndRot();
        wasEyeInWater = isEyeInFluid(FluidTags.WATER);
        refreshWaterState();
        updateSwimming();
        if (jumpCooldown > 0) {
            jumpCooldown--;
        }
        Vec3 velocity = getDeltaMovement();
        double velocityX = velocity.x;
        double velocityY = velocity.y;
        double velocityZ = velocity.z;
        if (velocity.horizontalDistanceSqr() < 9.0E-6) {
            velocityX = 0.0;
            velocityZ = 0.0;
        }
        if (Math.abs(velocity.y) < 0.003) {
            velocityY = 0.0;
        }
        setDeltaMovement(velocityX, velocityY, velocityZ);
        updateJumping();
        travel(new Vec3(xxa, yya, zza));
        applyEffectsFromBlocks();
    }

    private void updateJumping() {
        if (!jumping || !isAffectedByFluids()) {
            jumpCooldown = 0;
            return;
        }
        double fluidHeight = isInLava() ? getFluidHeight(FluidTags.LAVA) : getFluidHeight(FluidTags.WATER);
        boolean touchingWater = isInWater() && fluidHeight > 0.0;
        double swimHeight = getFluidJumpThreshold();
        if (touchingWater && (!onGround() || fluidHeight > swimHeight)) {
            jumpInLiquid(FluidTags.WATER);
        } else if (isInLava() && (!onGround() || fluidHeight > swimHeight)) {
            jumpInLiquid(FluidTags.LAVA);
        } else if ((onGround() || touchingWater && fluidHeight <= swimHeight) && jumpCooldown == 0) {
            jumpFromGround();
            jumpCooldown = 10;
        }
    }

    @Override
    public boolean isCreative() {
        return sourcePlayer != null && sourcePlayer.isCreative();
    }

    @Override
    public boolean hasEffect(Holder<MobEffect> effect) {
        return sourcePlayer != null && sourcePlayer.hasEffect(effect);
    }

    @Override
    public MobEffectInstance getEffect(Holder<MobEffect> effect) {
        return sourcePlayer == null ? null : sourcePlayer.getEffect(effect);
    }

    @Override
    public Collection<MobEffectInstance> getActiveEffects() {
        return sourcePlayer == null ? List.of() : sourcePlayer.getActiveEffects();
    }
}
