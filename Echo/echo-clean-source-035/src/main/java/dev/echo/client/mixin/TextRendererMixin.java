package dev.echo.client.mixin;

import dev.echo.client.events.render.TextTransformEvent;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Font.class)
public abstract class TextRendererMixin {
    @ModifyVariable(method = "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;",
        at = @At("HEAD"), argsOnly = true)
    private String modifyPrepare(String text) {
        return TextTransformEvent.transform(text);
    }

    @ModifyVariable(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
        at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence modifyPrepare(FormattedCharSequence text) {
        return TextTransformEvent.transform(text);
    }

    @ModifyVariable(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true)
    private String modifyGetWidth(String text) {
        return TextTransformEvent.transform(text);
    }

    @ModifyVariable(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), argsOnly = true)
    private FormattedText modifyGetWidth(FormattedText text) {
        return TextTransformEvent.transform(text);
    }

    @ModifyVariable(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence modifyGetWidth(FormattedCharSequence text) {
        return TextTransformEvent.transform(text);
    }
}
