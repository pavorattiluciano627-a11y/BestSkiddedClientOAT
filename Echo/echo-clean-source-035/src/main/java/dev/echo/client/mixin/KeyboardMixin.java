package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.input.KeyboardEvent;
import dev.echo.client.input.InputKey;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"))
    public void postKeyEvent(long window, int action, KeyInput input, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (!InputKey.ESCAPE.matches(input.key())) {
            new KeyboardEvent(input.key(), action).post();
        }
    }
}
