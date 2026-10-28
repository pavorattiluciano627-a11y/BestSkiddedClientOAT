package dev.echo.compat.lwjgl.mixin;

import dev.echo.compat.lwjgl.LegacyTinyFileDialogs;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps the constructor's original error-dialog boolean behavior. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftLegacyDialogMixin {
    @Redirect(method = "<init>(Lnet/minecraft/client/RunArgs;)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/tinyfd/TinyFileDialogs;tinyfd_messageBox(Ljava/lang/CharSequence;Ljava/lang/CharSequence;Ljava/lang/CharSequence;Ljava/lang/CharSequence;Z)Z", remap = false),
            require = 2, expect = 2, allow = 2)
    private static boolean echo$legacyMessageBox(CharSequence title, CharSequence message,
            CharSequence dialogType, CharSequence iconType, boolean defaultButton) {
        return LegacyTinyFileDialogs.messageBox(title, message, dialogType, iconType, defaultButton);
    }
}
