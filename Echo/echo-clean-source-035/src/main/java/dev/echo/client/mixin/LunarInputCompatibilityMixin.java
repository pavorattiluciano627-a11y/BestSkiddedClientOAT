package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.input.KeyStateOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional compatibility with the exact externally obfuscated Lunar input API. */
@Pseudo
@Mixin(targets = "com/moonsworth/lunar/IHIRRHORRRRHICCOHOHORCHIHHHIOO/CICOHIIRRHHIIHRROHOICOCRHHHHOO/CRRHRIRIHOHRHRCCHRCOICHOOIHOIC", remap = false)
public abstract class LunarInputCompatibilityMixin {
    // External compatibility selector, deliberately literal and not remapped by Yarn.
    @Inject(
        method = "CRRHRIRIHOHRHRCCHRCOICHOOIHOIC(Lcom/moonsworth/lunar/IHHHICHHRICRCIOHCIOHHOHIOIORRH/RCHOIOIIRCHIRRHIIHHRHOORCCIHCH/CICOHIIRRHHIIHRROHOICOCRHHHHOO;)Z",
        at = @At("HEAD"), cancellable = true, require = 0
    )
    private void overrideLunarKeyState(@Coerce Object lunarKey, CallbackInfoReturnable<Boolean> callback) {
        if (!Echo.eventsDisabled) {
            Boolean overriddenState = KeyStateOverrides.forBoundKey(lunarKey);
            if (overriddenState != null) {
                callback.setReturnValue(overriddenState);
            }
        }
    }
}
