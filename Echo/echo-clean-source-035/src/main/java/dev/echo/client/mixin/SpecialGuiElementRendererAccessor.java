package dev.echo.client.mixin;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Texture view produced by the special GUI element renderer. */
@Mixin(SpecialGuiElementRenderer.class)
public interface SpecialGuiElementRendererAccessor {
    @Accessor("textureView")
    GpuTextureView getTextureView();
}
