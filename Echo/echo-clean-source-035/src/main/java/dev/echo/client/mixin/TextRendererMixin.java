package dev.echo.client.mixin;

import dev.echo.client.events.render.TextTransformEvent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextRenderer.class)
public abstract class TextRendererMixin {
    @ModifyVariable(method = "prepare(Ljava/lang/String;FFIZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;",
        at = @At("HEAD"), argsOnly = true)
    private String modifyPrepare(String text) {
        return TextTransformEvent.transform(text);
    }

    @ModifyVariable(method = "prepare(Lnet/minecraft/text/OrderedText;FFIZZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;",
        at = @At("HEAD"), argsOnly = true)
    private OrderedText modifyPrepare(OrderedText text) {
        return TextTransformEvent.transform(text);
    }

    @ModifyVariable(method = "getWidth(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true)
    private String modifyGetWidth(String text) {
        return TextTransformEvent.transform(text);
    }

    @ModifyVariable(method = "getWidth(Lnet/minecraft/text/StringVisitable;)I", at = @At("HEAD"), argsOnly = true)
    private StringVisitable modifyGetWidth(StringVisitable text) {
        return TextTransformEvent.transform(text);
    }

    @ModifyVariable(method = "getWidth(Lnet/minecraft/text/OrderedText;)I", at = @At("HEAD"), argsOnly = true)
    private OrderedText modifyGetWidth(OrderedText text) {
        return TextTransformEvent.transform(text);
    }
}
