package dev.echo.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.echo.client.events.input.AttackKeyEvent;
import dev.echo.client.events.input.KeybindingEvent;
import dev.echo.client.events.input.UseKeyEvent;
import dev.echo.client.mixin.KeyBindingAccessor;
import dev.echo.client.mixin.MouseAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;

/** Replays extra attack/use clicks through Minecraft's original mouse input handler. */
public final class InputState {
    private InputState() {
    }

    /** Applies positive, uncancelled click-count deltas only while no screen is open. */
    public static void applyExtraClicks(KeybindingEvent event) {
        if (event.cancelled || event.getClickCountDelta() <= 0
                || Minecraft.getInstance().gui.screen() != null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (event instanceof AttackKeyEvent) {
            replayMouseClicks(client, client.options.keyAttack, event.getClickCountDelta());
        } else if (event instanceof UseKeyEvent) {
            replayMouseClicks(client, client.options.keyUse, event.getClickCountDelta());
        }
    }

    private static void replayMouseClicks(Minecraft client, KeyMapping binding, int count) {
        if (KeybindingCache.isPressed(binding)) {
            return;
        }
        KeyStateOverrides.setPressed(binding, true);
        InputConstants.Key key = ((KeyBindingAccessor) binding).getBoundKey();
        if (key.getType() != InputConstants.Type.MOUSE) {
            return;
        }
        boolean wasPressed = binding.isDown();
        long handle = client.getWindow().handle();
        MouseButtonInfo input = new MouseButtonInfo(key.getValue(), 0);
        for (int i = 0; i < count; i++) {
            ((MouseAccessor) client.mouseHandler).invokeOnMouseButton(handle, input, GLFW.GLFW_PRESS);
            ((MouseAccessor) client.mouseHandler).invokeOnMouseButton(handle, input, GLFW.GLFW_RELEASE);
        }
        // Preserve the original normal-completion restoration, including exception behavior.
        binding.setDown(wasPressed);
    }
}
