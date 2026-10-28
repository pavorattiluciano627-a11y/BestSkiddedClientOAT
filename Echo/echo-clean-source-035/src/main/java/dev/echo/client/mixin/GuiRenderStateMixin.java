package dev.echo.client.mixin;

import dev.echo.client.bridge.GuiStateControl;
import java.util.List;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
public class GuiRenderStateMixin implements GuiStateControl {
    @Shadow @Final private List<?> rootLayers;
    @Shadow private int blurLayer;
    @Unique private int screenLayerStart = -1;
    @Unique private boolean screenBlurLayerInstalled;

    @Inject(method = "clear", at = @At("TAIL"))
    private void onClearTail(CallbackInfo callback) {
        screenLayerStart = -1;
        screenBlurLayerInstalled = false;
    }

    @Override
    public void markScreenLayerStart() {
        if (screenLayerStart == -1) {
            screenLayerStart = rootLayers.size();
        }
    }

    @Override
    public boolean prepareScreenBlurLayer() {
        if (screenLayerStart == -1 || screenLayerStart >= rootLayers.size()) {
            return false;
        }
        if (blurLayer == Integer.MAX_VALUE) {
            blurLayer = screenLayerStart;
            screenBlurLayerInstalled = true;
        }
        return true;
    }

    @Override
    public boolean isScreenBlurLayerInstalled() {
        return screenBlurLayerInstalled;
    }
}
