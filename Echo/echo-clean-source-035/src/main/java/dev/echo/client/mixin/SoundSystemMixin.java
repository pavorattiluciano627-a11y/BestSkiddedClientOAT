package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.utils.audio.EchoAudio;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
    @Inject(method = "reloadSounds()V", at = @At("TAIL"))
    private void onReloadSoundsTail(CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            EchoAudio.rebuildBuffers();
        }
    }

    @Inject(method = "tick(Z)V", at = @At("HEAD"))
    private void onTickHead(boolean paused, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            EchoAudio.tick();
        }
    }
}
