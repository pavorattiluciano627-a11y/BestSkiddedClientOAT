package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.input.MouseMovementEvent;
import dev.echo.client.input.KeybindingCache;
import dev.echo.client.render.RenderBackendManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @ModifyVariable(method = "onButton", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private long normalizeButtonWindow(long window) {
        return echo$normalizeWindow(window);
    }

    @Inject(method = "onButton", at = @At("HEAD"))
    private void cacheMouseButton(long window, MouseButtonInfo input, int action, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        KeybindingCache.setMousePressed(input.button(), action == GLFW.GLFW_PRESS);
    }

    @ModifyVariable(method = "onMove", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private long normalizeCursorWindow(long window) {
        return echo$normalizeWindow(window);
    }

    @ModifyVariable(method = "onScroll", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private long normalizeScrollWindow(long window) {
        return echo$normalizeWindow(window);
    }

    @Unique
    private static long echo$normalizeWindow(long window) {
        RenderBackendManager backend = Echo.externalRenderer;
        return backend != null && window == backend.windowHandle()
                ? Minecraft.getInstance().getWindow().handle() : window;
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void updateMouseDeltas(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        // PipelineResolver is the HEAD/Pre producer; preserve delta write-back here.
        MouseMovementEvent.Pre event = new MouseMovementEvent.Pre(accumulatedDX, accumulatedDY);
        event.post();
        accumulatedDX = event.deltaX;
        accumulatedDY = event.deltaY;
    }

    @Inject(method = "turnPlayer", at = @At("TAIL"), cancellable = true)
    private void afterMouseUpdate(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        MouseMovementEvent.Post event = new MouseMovementEvent.Post(accumulatedDX, accumulatedDY);
        event.post();
        if (event.cancelled) {
            callback.cancel();
        }
    }
}
