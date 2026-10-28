package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.input.KeyPressedQueryEvent;
import dev.echo.client.events.input.PlayerInputEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies explicit sprint-query overrides before the current processed input snapshot. */
@Mixin(KeyMapping.class)
public class KeyBindingMixin {
    @Shadow
    private boolean isDown;

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void onIsPressedHead(CallbackInfoReturnable<Boolean> callback) {
        if (!Echo.eventsDisabled) {
            Minecraft client = Minecraft.getInstance();
            if (client.options != null && client.player != null) {
                if ((Object) this == client.options.keySprint) {
                    KeyPressedQueryEvent event = new KeyPressedQueryEvent(isDown);
                    event.post();
                    if (event.isOverridden()) {
                        callback.setReturnValue(event.isPressed());
                        return;
                    }
                }

                PlayerInputEvent input = PlayerInputEvent.lastInput;
                if (PlayerInputEvent.inputProcessed && input != null) {
                    if ((Object) this == client.options.keyUp) {
                        callback.setReturnValue(input.forward);
                    } else if ((Object) this == client.options.keyDown) {
                        callback.setReturnValue(input.backward);
                    } else if ((Object) this == client.options.keyLeft) {
                        callback.setReturnValue(input.left);
                    } else if ((Object) this == client.options.keyRight) {
                        callback.setReturnValue(input.right);
                    } else if ((Object) this == client.options.keyJump) {
                        callback.setReturnValue(input.jump);
                    } else if ((Object) this == client.options.keyShift) {
                        callback.setReturnValue(input.sneak);
                    } else if ((Object) this == client.options.keySprint) {
                        callback.setReturnValue(input.sprint);
                    }
                }
            }
        }
    }
}
