package dev.echo.compat.lwjgl.mixin;

import dev.echo.compat.lwjgl.LegacyTinyFileDialogs;
import net.minecraft.client.GameNarrator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps narrator initialization's error-dialog decision unchanged. */
@Mixin(GameNarrator.class)
public abstract class NarratorLegacyDialogMixin {
    @Redirect(method = "checkStatus(Z)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/tinyfd/TinyFileDialogs;tinyfd_messageBox(Ljava/lang/CharSequence;Ljava/lang/CharSequence;Ljava/lang/CharSequence;Ljava/lang/CharSequence;Z)Z", remap = false),
            require = 1, expect = 1, allow = 1)
    private static boolean echo$legacyMessageBox(CharSequence title, CharSequence message,
            CharSequence dialogType, CharSequence iconType, boolean defaultButton) {
        return LegacyTinyFileDialogs.messageBox(title, message, dialogType, iconType, defaultButton);
    }
}
