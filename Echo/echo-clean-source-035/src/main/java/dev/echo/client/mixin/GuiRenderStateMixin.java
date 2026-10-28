package dev.echo.client.mixin;

import dev.echo.client.bridge.GuiStateControl;
import java.util.List;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
public class GuiRenderStateMixin implements GuiStateControl {
    @Shadow @Final private List<?> strata;
    @Shadow private int firstStratumAfterBlur;
    @Unique private int screenLayerStart = -1;
    @Unique private boolean screenBlurLayerInstalled;

    @Inject(method = "reset", at = @At("TAIL"))
    private void onClearTail(CallbackInfo callback) {
        screenLayerStart = -1;
        screenBlurLayerInstalled = false;
    }

    @Override
    public void markScreenLayerStart() {
        if (screenLayerStart == -1) {
            screenLayerStart = strata.size();
        }
    }

    @Override
    public boolean prepareScreenBlurLayer() {
        if (screenLayerStart == -1 || screenLayerStart >= strata.size()) {
            return false;
        }
        if (firstStratumAfterBlur == Integer.MAX_VALUE) {
            firstStratumAfterBlur = screenLayerStart;
            screenBlurLayerInstalled = true;
        }
        return true;
    }

    @Override
    public boolean isScreenBlurLayerInstalled() {
        return screenBlurLayerInstalled;
    }
}
