package dev.echo.compat.lwjgl.mixin;

import com.mojang.blaze3d.platform.MessageBox;
import dev.echo.compat.lwjgl.LegacyTinyFileDialogs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 26.2 moved every crash/ error dialog into {@code com.mojang.blaze3d.platform
 * .MessageBox}; Minecraft, Window and GameNarrator no longer call
 * {@code tinyfd_messageBox} themselves. The same call is redirected here,
 * matching LWJGL 3.4's integer-returning overload.
 */
@Mixin(MessageBox.class)
public abstract class MessageBoxLegacyDialogMixin {
    @Redirect(method = {"error", "errorWithContinue"},
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/util/tinyfd/TinyFileDialogs;tinyfd_messageBox(Ljava/lang/CharSequence;Ljava/lang/CharSequence;Ljava/lang/CharSequence;Ljava/lang/CharSequence;I)I",
                    remap = false),
            require = 0)
    private static int echo$legacyMessageBox(CharSequence title, CharSequence message,
            CharSequence dialogType, CharSequence iconType, int defaultButton) {
        return LegacyTinyFileDialogs.messageBox(title, message, dialogType, iconType,
                defaultButton != 0) ? 1 : 0;
    }
}
