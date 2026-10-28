package dev.echo.client.handlers;

import dev.echo.client.Echo;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.KeyboardEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.input.InputKey;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.glfw.GLFW;

/**
 * Complete structured candidate held in analysis until InputKey.isPressed's
 * captured-lambda target and the listed external method names are resolved.
 */
public class FeatureBindHandler extends ClientHandler {
    private final Map<Integer, Boolean> previousMouseStates = new HashMap<>();

    @Subscribe
    public void onKeyboard(KeyboardEvent event) {
        if (Echo.featureManager == null) {
            return;
        }
        boolean pressed = event.getAction() == GLFW.GLFW_PRESS;
        if (!pressed && event.getAction() != GLFW.GLFW_RELEASE) {
            return;
        }
        if (pressed && CLIENT.gui.screen() != null) {
            return;
        }
        for (Feature feature : Echo.featureManager.getFeatures()) {
            if (!feature.isKeybindable()) {
                continue;
            }
            InputKey key = feature.getKey();
            if (key == InputKey.NONE || key != InputKey.fromCode(event.getKey())) {
                continue;
            }
            if (feature.isHoldMode()) {
                feature.setEnabled(pressed);
            } else if (pressed) {
                feature.toggle();
            }
        }
    }

    @Subscribe
    public void onTick(ClientTickEvent event) {
        if (CLIENT.gui.screen() != null || Echo.featureManager == null) {
            return;
        }
        Map<Integer, Boolean> currentMouseStates = new HashMap<>();
        for (Feature feature : Echo.featureManager.getFeatures()) {
            if (!feature.isKeybindable()) {
                continue;
            }
            InputKey key = feature.getKey();
            if (key == InputKey.NONE || !key.isMouse()) {
                continue;
            }
            int inputCode = key.code();
            boolean pressed = currentMouseStates.computeIfAbsent(inputCode, ignored -> key.isPressed());
            if (feature.isHoldMode()) {
                feature.setEnabled(pressed);
            } else if (pressed && !previousMouseStates.getOrDefault(inputCode, false)) {
                feature.toggle();
            }
        }
        previousMouseStates.putAll(currentMouseStates);
    }
}
