package dev.echo.client.utils.simulation;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.GameEvent;

/**
 * The original prediction player's movement capabilities and world-effect policy.
 * Physics and copied player state are supplied by the concrete simulation player.
 */
public abstract class SimulationPlayerBase extends ClientPlayerCopy {
    protected SimulationPlayerBase(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Override
    public boolean canMoveVoluntarily() {
        return true;
    }

    @Override
    public boolean canActVoluntarily() {
        return true;
    }

    @Override
    protected boolean isControlledByMainPlayer() {
        return true;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    protected Entity.MoveEffect getMoveEffect() {
        return Entity.MoveEffect.NONE;
    }

    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
        // The original prediction override intentionally suppresses landing effects.
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        // The original prediction override intentionally suppresses sound playback.
    }

    @Override
    public void emitGameEvent(RegistryEntry<GameEvent> event, Entity source) {
        // The original prediction override intentionally suppresses world-listener events.
    }

    @Override
    public boolean clientDamage(DamageSource source) {
        return false;
    }

    @Override
    public void pushAwayFrom(Entity other) {
        // The original prediction override intentionally leaves other entities untouched.
    }

    @Override
    protected void onSwimmingStart() {
        // The original prediction override intentionally suppresses splash effects.
    }
}
