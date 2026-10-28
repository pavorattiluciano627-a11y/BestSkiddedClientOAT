package dev.echo.client.input;

import dev.echo.client.events.input.AttackKeyEvent;
import dev.echo.client.events.input.KeybindingEvent;
import dev.echo.client.events.input.UseKeyEvent;
import dev.echo.client.mixin.KeyBindingAccessor;
import dev.echo.client.mixin.MouseAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Replays extra attack/use clicks through Minecraft's original mouse input handler. */
public final class InputState {
    private InputState() {
    }

    /** Applies positive, uncancelled click-count deltas only while no screen is open. */
    public static void applyExtraClicks(KeybindingEvent event) {
        if (event.cancelled || event.getClickCountDelta() <= 0
                || MinecraftClient.getInstance().currentScreen != null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (event instanceof AttackKeyEvent) {
            replayMouseClicks(client, client.options.attackKey, event.getClickCountDelta());
        } else if (event instanceof UseKeyEvent) {
            replayMouseClicks(client, client.options.useKey, event.getClickCountDelta());
        }
    }

    private static void replayMouseClicks(MinecraftClient client, KeyBinding binding, int count) {
        if (KeybindingCache.isPressed(binding)) {
            return;
        }
        KeyStateOverrides.setPressed(binding, true);
        InputUtil.Key key = ((KeyBindingAccessor) binding).getBoundKey();
        if (key.getCategory() != InputUtil.Type.MOUSE) {
            return;
        }
        boolean wasPressed = binding.isPressed();
        long handle = client.getWindow().getHandle();
        MouseInput input = new MouseInput(key.getCode(), 0);
        for (int i = 0; i < count; i++) {
            ((MouseAccessor) client.mouse).invokeOnMouseButton(handle, input, GLFW.GLFW_PRESS);
            ((MouseAccessor) client.mouse).invokeOnMouseButton(handle, input, GLFW.GLFW_RELEASE);
        }
        // Preserve the original normal-completion restoration, including exception behavior.
        binding.setPressed(wasPressed);
    }
}
