package dev.echo.client.mixin;

import dev.echo.client.bridge.GuiStateControl;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"))
    private void onRenderWithTooltipHead(GuiGraphicsExtractor context, int mouseX, int mouseY,
                                         float tickProgress, CallbackInfo callback) {
        ((GuiStateControl) context.guiRenderState).markScreenLayerStart();
    }
}
