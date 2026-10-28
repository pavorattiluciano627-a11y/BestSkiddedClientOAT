package dev.echo.client.utils.simulation;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.Vec3d;

/**
 * Complete functional simulation candidate for the documented normal control domain.
 * Dispatch/cache and opaque-state obligations are retained in the continuation ledger.
 */
public class SimulatedPlayer extends SimulationPlayerBase {
    private static final GameProfile SIMULATION_PROFILE = new GameProfile(
            UUID.nameUUIDFromBytes("echo:simulated-player".getBytes(StandardCharsets.UTF_8)), "echo_sim");
    private PlayerEntity sourcePlayer;
    private int jumpCooldown;

    public SimulatedPlayer(ClientWorld world) {
        super(world, SIMULATION_PROFILE);
        noClip = false;
        firstUpdate = false;
        setInvisible(true);
        setSilent(true);
    }

    public void copyFrom(PlayerEntity player, Vec3d velocity, float yaw, float pitch,
            float forwardInput, float sidewaysInput, boolean jumpPressed) {
        sourcePlayer = player;
        getAbilities().unpack(player.getAbilities().pack());
        setSprinting(player.isSprinting());
        getAttributes().setFrom(player.getAttributes());
        setMovementSpeed(player.getMovementSpeed());
        setPose(player.getPose());
        calculateDimensions();
        setPosition(player.getX(), player.getY(), player.getZ());
        resetPosition();
        setVelocity(velocity);
        setOnGround(player.isOnGround());
        horizontalCollision = player.horizontalCollision;
        verticalCollision = player.verticalCollision;
        groundCollision = player.groundCollision;
        collidedSoftly = player.collidedSoftly;
        fallDistance = player.fallDistance;
        movementMultiplier = Vec3d.ZERO;
        setYaw(yaw);
        setPitch(pitch);
        setHeadYaw(yaw);
        setBodyYaw(yaw);
        setSneaking(player.isSneaking());
        setSwimming(player.isSwimming());
        // The original Entity invoker forwards exactly to this protected setter.
        setFlag(7, player.isGliding());
        sidewaysSpeed = sidewaysInput;
        upwardSpeed = 0.0F;
        forwardSpeed = forwardInput;
        jumping = jumpPressed;
        jumpCooldown = 0;
    }

    public void tickPrediction() {
        resetPosition();
        submergedInWater = isSubmergedIn(FluidTags.WATER);
        refreshWaterState();
        updateSwimming();
        if (jumpCooldown > 0) {
            jumpCooldown--;
        }
        Vec3d velocity = getVelocity();
        double velocityX = velocity.x;
        double velocityY = velocity.y;
        double velocityZ = velocity.z;
        if (velocity.horizontalLengthSquared() < 9.0E-6) {
            velocityX = 0.0;
            velocityZ = 0.0;
        }
        if (Math.abs(velocity.y) < 0.003) {
            velocityY = 0.0;
        }
        setVelocity(velocityX, velocityY, velocityZ);
        updateJumping();
        travel(new Vec3d(sidewaysSpeed, upwardSpeed, forwardSpeed));
        tickBlockCollision();
    }

    private void updateJumping() {
        if (!jumping || !shouldSwimInFluids()) {
            jumpCooldown = 0;
            return;
        }
        double fluidHeight = isInLava() ? getFluidHeight(FluidTags.LAVA) : getFluidHeight(FluidTags.WATER);
        boolean touchingWater = isTouchingWater() && fluidHeight > 0.0;
        double swimHeight = getSwimHeight();
        if (touchingWater && (!isOnGround() || fluidHeight > swimHeight)) {
            swimUpward(FluidTags.WATER);
        } else if (isInLava() && (!isOnGround() || fluidHeight > swimHeight)) {
            swimUpward(FluidTags.LAVA);
        } else if ((isOnGround() || touchingWater && fluidHeight <= swimHeight) && jumpCooldown == 0) {
            jump();
            jumpCooldown = 10;
        }
    }

    @Override
    public boolean isCreative() {
        return sourcePlayer != null && sourcePlayer.isCreative();
    }

    @Override
    public boolean hasStatusEffect(RegistryEntry<StatusEffect> effect) {
        return sourcePlayer != null && sourcePlayer.hasStatusEffect(effect);
    }

    @Override
    public StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> effect) {
        return sourcePlayer == null ? null : sourcePlayer.getStatusEffect(effect);
    }

    @Override
    public Collection<StatusEffectInstance> getStatusEffects() {
        return sourcePlayer == null ? List.of() : sourcePlayer.getStatusEffects();
    }
}
