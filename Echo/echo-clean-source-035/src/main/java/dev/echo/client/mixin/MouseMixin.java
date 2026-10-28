package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.input.MouseMovementEvent;
import dev.echo.client.input.KeybindingCache;
import dev.echo.client.render.RenderBackendManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Shadow private double cursorDeltaX;
    @Shadow private double cursorDeltaY;

    @ModifyVariable(method = "onMouseButton", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private long normalizeButtonWindow(long window) {
        return echo$normalizeWindow(window);
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void cacheMouseButton(long window, MouseInput input, int action, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        KeybindingCache.setMousePressed(input.button(), action == GLFW.GLFW_PRESS);
    }

    @ModifyVariable(method = "onCursorPos", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private long normalizeCursorWindow(long window) {
        return echo$normalizeWindow(window);
    }

    @ModifyVariable(method = "onMouseScroll", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private long normalizeScrollWindow(long window) {
        return echo$normalizeWindow(window);
    }

    @Unique
    private static long echo$normalizeWindow(long window) {
        RenderBackendManager backend = Echo.externalRenderer;
        return backend != null && window == backend.windowHandle()
                ? MinecraftClient.getInstance().getWindow().getHandle() : window;
    }

    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void updateMouseDeltas(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        // PipelineResolver is the HEAD/Pre producer; preserve delta write-back here.
        MouseMovementEvent.Pre event = new MouseMovementEvent.Pre(cursorDeltaX, cursorDeltaY);
        event.post();
        cursorDeltaX = event.deltaX;
        cursorDeltaY = event.deltaY;
    }

    @Inject(method = "updateMouse", at = @At("TAIL"), cancellable = true)
    private void afterMouseUpdate(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        MouseMovementEvent.Post event = new MouseMovementEvent.Post(cursorDeltaX, cursorDeltaY);
        event.post();
        if (event.cancelled) {
            callback.cancel();
        }
    }
}
