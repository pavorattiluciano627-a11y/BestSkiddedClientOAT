package dev.echo.client.mixin;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Texture view produced by the special GUI element renderer. */
@Mixin(PictureInPictureRenderer.class)
public interface SpecialGuiElementRendererAccessor {
    @Accessor("textureView")
    GpuTextureView getTextureView();
}
