package dev.echo.client.utils.simulation;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * The original prediction player's movement capabilities and world-effect policy.
 * Physics and copied player state are supplied by the concrete simulation player.
 */
public abstract class SimulationPlayerBase extends ClientPlayerCopy {
    protected SimulationPlayerBase(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Override
    public boolean canSimulateMovement() {
        return true;
    }

    @Override
    public boolean isEffectiveAi() {
        return true;
    }

    @Override
    protected boolean isLocalClientAuthoritative() {
        return true;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
        // The original prediction override intentionally suppresses landing effects.
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        // The original prediction override intentionally suppresses sound playback.
    }

    @Override
    public void gameEvent(Holder<GameEvent> event, Entity source) {
        // The original prediction override intentionally suppresses world-listener events.
    }

    @Override
    public boolean hurtClient(DamageSource source) {
        return false;
    }

    @Override
    public void push(Entity other) {
        // The original prediction override intentionally leaves other entities untouched.
    }

    @Override
    protected void doWaterSplashEffect() {
        // The original prediction override intentionally suppresses splash effects.
    }
}
