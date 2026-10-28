package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.input.KeyStateOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Candidate: key-free KeyStateOverrides contract requires callee proof. */
@Pseudo
@Mixin(targets = LunarKeyStateCompatibilityMixin.EXTERNAL_LUNAR_KEY_STATE_CLASS, remap = false)
public class LunarKeyStateCompatibilityMixin {
    /** Literal identifiers in the supported external Lunar build. */
    public static final String EXTERNAL_LUNAR_KEY_STATE_CLASS =
            "com/moonsworth/lunar/IHIRRHORRRRHICCOHOHORCHIHHHIOO/"
            + "CICOHIIRRHHIIHRROHOICOCRHHHHOO/CICOHIIRRHHIIHRROHOICOCRHHHHOO";
    public static final String EXTERNAL_LUNAR_KEY_STATE_METHOD = "IHICROHHIOORRHIICCRCHICRROICIR(I)Z";

    @Inject(method = EXTERNAL_LUNAR_KEY_STATE_METHOD, at = @At("HEAD"), cancellable = true, require = 0)
    private void onExternalKeyStateQuery(int keyCode, CallbackInfoReturnable<Boolean> callback) {
        if (!Echo.eventsDisabled) {
            Boolean override = KeyStateOverrides.pressedActionForMouseButton(keyCode);
            if (override != null) {
                callback.setReturnValue(override);
            }
        }
    }
}
