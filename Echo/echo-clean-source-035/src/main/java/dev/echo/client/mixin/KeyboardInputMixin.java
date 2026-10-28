package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.features.impl.render.FreecamFeature;
import dev.echo.client.input.KeybindingCache;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Shadow @Final private GameOptions settings;

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTickHead(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            clearInputOverrides();
            return;
        }
        PlayerInputEvent.inputProcessed = false;
        PlayerInputEvent event = new PlayerInputEvent(
            settings.forwardKey.isPressed(), settings.backKey.isPressed(),
            settings.leftKey.isPressed(), settings.rightKey.isPressed(),
            settings.jumpKey.isPressed(), settings.sneakKey.isPressed(),
            settings.sprintKey.isPressed()
        );
        event.post();
        PlayerInputEvent.lastInput = event;
        PlayerInputEvent.inputProcessed = true;
        updateKeyOverride(settings.forwardKey, event.forward, event.originalInput.forward());
        updateKeyOverride(settings.backKey, event.backward, event.originalInput.backward());
        updateKeyOverride(settings.leftKey, event.left, event.originalInput.left());
        updateKeyOverride(settings.rightKey, event.right, event.originalInput.right());
        updateKeyOverride(settings.jumpKey, event.jump, event.originalInput.jump());
        updateKeyOverride(settings.sneakKey, event.sneak, event.originalInput.sneak());
        updateKeyOverride(settings.sprintKey, event.sprint, event.originalInput.sprint());
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void onTickReturn(CallbackInfo callback) {
        PlayerInputEvent event = PlayerInputEvent.lastInput;
        if (!Echo.eventsDisabled && event != null) {
            FreecamFeature freecam = Echo.featureManager == null
                ? null : (FreecamFeature) Echo.featureManager.findFeature(FreecamFeature.class);
            if (freecam != null && freecam.isEnabled() && !event.allowMovementInFreecam) {
                playerInput = new PlayerInput(false, false, false, false, false, false, false);
                movementVector = Vec2f.ZERO;
            }
        }
    }

    @Unique
    private void updateKeyOverride(KeyBinding binding, boolean overridden, boolean original) {
        InputUtil.Key key = ((KeyBindingAccessor) binding).getBoundKey();
        if (key != null && key.getCategory() == InputUtil.Type.KEYSYM) {
            KeybindingCache.updateOverride(key.getCode(), overridden, original);
        }
    }

    @Unique
    private void clearInputOverrides() {
        PlayerInputEvent.lastInput = null;
        PlayerInputEvent.inputProcessed = false;
        KeybindingCache.clearOverrides();
    }
}
