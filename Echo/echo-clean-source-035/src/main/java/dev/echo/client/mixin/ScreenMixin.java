package dev.echo.client.mixin;

import dev.echo.client.bridge.GuiStateControl;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "renderWithTooltip", at = @At("HEAD"))
    private void onRenderWithTooltipHead(DrawContext context, int mouseX, int mouseY,
                                         float tickProgress, CallbackInfo callback) {
        ((GuiStateControl) context.state).markScreenLayerStart();
    }
}
