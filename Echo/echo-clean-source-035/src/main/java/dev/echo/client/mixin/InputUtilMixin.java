package dev.echo.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.echo.client.input.KeybindingCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InputConstants.class)
public class InputUtilMixin {
    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    private static void useCachedKeyState(Window window, int keyCode,
            CallbackInfoReturnable<Boolean> callback) {
        Boolean pressed = KeybindingCache.getOverride(keyCode);
        if (pressed != null) {
            callback.setReturnValue(pressed);
        }
    }
}
