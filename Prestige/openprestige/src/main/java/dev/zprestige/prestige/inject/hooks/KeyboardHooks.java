package dev.zprestige.prestige.inject.hooks;

import dev.zprestige.prestige.ui.mc.Overlay;
import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

/** Injection-mode twin of ui.mixin.KeyboardHandlerMixin: swallows keyboard input while the overlay is visible. */
@CTransformer({Keyboard.class})
public class KeyboardHooks {
    @CInject(method = {"onKey"}, target = {@CTarget("HEAD")}, cancellable = true)
    private void key(long window, int action, KeyInput ev, InjectionCallback ci) {
        if (Overlay.onKey(ev.key(), action)) ci.cancel();
    }

    @CInject(method = {"onChar"}, target = {@CTarget("HEAD")}, cancellable = true)
    private void character(long window, CharInput ev, InjectionCallback ci) {
        if (Overlay.onChar(ev.codepoint())) ci.cancel();
    }
}
