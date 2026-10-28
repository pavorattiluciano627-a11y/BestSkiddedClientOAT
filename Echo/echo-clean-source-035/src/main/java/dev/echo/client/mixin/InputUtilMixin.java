package dev.echo.client.mixin;

import dev.echo.client.input.KeybindingCache;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InputUtil.class)
public class InputUtilMixin {
    @Inject(method = "isKeyPressed", at = @At("HEAD"), cancellable = true)
    private static void useCachedKeyState(Window window, int keyCode,
            CallbackInfoReturnable<Boolean> callback) {
        Boolean pressed = KeybindingCache.getOverride(keyCode);
        if (pressed != null) {
            callback.setReturnValue(pressed);
        }
    }
}
