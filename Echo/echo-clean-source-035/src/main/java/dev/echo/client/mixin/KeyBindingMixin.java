package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.input.KeyPressedQueryEvent;
import dev.echo.client.events.input.PlayerInputEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies explicit sprint-query overrides before the current processed input snapshot. */
@Mixin(KeyBinding.class)
public class KeyBindingMixin {
    @Shadow
    private boolean pressed;

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void onIsPressedHead(CallbackInfoReturnable<Boolean> callback) {
        if (!Echo.eventsDisabled) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.options != null && client.player != null) {
                if ((Object) this == client.options.sprintKey) {
                    KeyPressedQueryEvent event = new KeyPressedQueryEvent(pressed);
                    event.post();
                    if (event.isOverridden()) {
                        callback.setReturnValue(event.isPressed());
                        return;
                    }
                }

                PlayerInputEvent input = PlayerInputEvent.lastInput;
                if (PlayerInputEvent.inputProcessed && input != null) {
                    if ((Object) this == client.options.forwardKey) {
                        callback.setReturnValue(input.forward);
                    } else if ((Object) this == client.options.backKey) {
                        callback.setReturnValue(input.backward);
                    } else if ((Object) this == client.options.leftKey) {
                        callback.setReturnValue(input.left);
                    } else if ((Object) this == client.options.rightKey) {
                        callback.setReturnValue(input.right);
                    } else if ((Object) this == client.options.jumpKey) {
                        callback.setReturnValue(input.jump);
                    } else if ((Object) this == client.options.sneakKey) {
                        callback.setReturnValue(input.sneak);
                    } else if ((Object) this == client.options.sprintKey) {
                        callback.setReturnValue(input.sprint);
                    }
                }
            }
        }
    }
}
