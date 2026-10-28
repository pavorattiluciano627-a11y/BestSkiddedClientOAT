package dev.echo.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.echo.client.Echo;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.features.impl.render.FreecamFeature;
import dev.echo.client.input.KeybindingCache;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
    @Shadow @Final private Options options;

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTickHead(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            clearInputOverrides();
            return;
        }
        PlayerInputEvent.inputProcessed = false;
        PlayerInputEvent event = new PlayerInputEvent(
            options.keyUp.isDown(), options.keyDown.isDown(),
            options.keyLeft.isDown(), options.keyRight.isDown(),
            options.keyJump.isDown(), options.keyShift.isDown(),
            options.keySprint.isDown()
        );
        event.post();
        PlayerInputEvent.lastInput = event;
        PlayerInputEvent.inputProcessed = true;
        updateKeyOverride(options.keyUp, event.forward, event.originalInput.forward());
        updateKeyOverride(options.keyDown, event.backward, event.originalInput.backward());
        updateKeyOverride(options.keyLeft, event.left, event.originalInput.left());
        updateKeyOverride(options.keyRight, event.right, event.originalInput.right());
        updateKeyOverride(options.keyJump, event.jump, event.originalInput.jump());
        updateKeyOverride(options.keyShift, event.sneak, event.originalInput.shift());
        updateKeyOverride(options.keySprint, event.sprint, event.originalInput.sprint());
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void onTickReturn(CallbackInfo callback) {
        PlayerInputEvent event = PlayerInputEvent.lastInput;
        if (!Echo.eventsDisabled && event != null) {
            FreecamFeature freecam = Echo.featureManager == null
                ? null : (FreecamFeature) Echo.featureManager.findFeature(FreecamFeature.class);
            if (freecam != null && freecam.isEnabled() && !event.allowMovementInFreecam) {
                keyPresses = new Input(false, false, false, false, false, false, false);
                moveVector = Vec2.ZERO;
            }
        }
    }

    @Unique
    private void updateKeyOverride(KeyMapping binding, boolean overridden, boolean original) {
        InputConstants.Key key = ((KeyBindingAccessor) binding).getBoundKey();
        if (key != null && key.getType() == InputConstants.Type.KEYSYM) {
            KeybindingCache.updateOverride(key.getValue(), overridden, original);
        }
    }

    @Unique
    private void clearInputOverrides() {
        PlayerInputEvent.lastInput = null;
        PlayerInputEvent.inputProcessed = false;
        KeybindingCache.clearOverrides();
    }
}
