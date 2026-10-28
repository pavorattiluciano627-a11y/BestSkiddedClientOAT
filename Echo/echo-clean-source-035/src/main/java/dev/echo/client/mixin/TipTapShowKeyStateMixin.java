package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.input.KeyStateOverrides;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Candidate: key-free KeyStateOverrides contract requires callee proof. */
@Pseudo
@Mixin(targets = TipTapShowKeyStateMixin.EXTERNAL_TIPTAP_RENDERABLE_BUTTON, remap = false)
public class TipTapShowKeyStateMixin {
    /** Literal third-party compatibility identifier, not an application symbol. */
    public static final String EXTERNAL_TIPTAP_RENDERABLE_BUTTON =
            "com/spyxar/tiptapshow/components/RenderableButton";

    @Redirect(method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z", remap = true),
            remap = false, require = 0)
    private boolean redirectDisplayedKeyState(KeyBinding binding) {
        return Echo.eventsDisabled ? binding.isPressed() : KeyStateOverrides.isPressed(binding);
    }
}
