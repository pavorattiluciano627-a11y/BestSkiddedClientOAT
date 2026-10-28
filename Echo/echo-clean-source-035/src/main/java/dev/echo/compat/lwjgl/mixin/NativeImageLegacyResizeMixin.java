package dev.echo.compat.lwjgl.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import dev.echo.compat.lwjgl.LegacyImageResize;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Retains Minecraft's pointer/stride calculation and STB1 resize semantics. */
@Mixin(NativeImage.class)
public abstract class NativeImageLegacyResizeMixin {
    @Redirect(method = "resizeSubRectTo(IIIILcom/mojang/blaze3d/platform/NativeImage;)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/stb/STBImageResize;nstbir_resize_uint8_linear(JIIIJIIII)J", remap = false),
            require = 0)
    private static long echo$legacyResize(long input, int inputWidth, int inputHeight,
            int inputStride, long output, int outputWidth, int outputHeight,
            int outputStride, int channels) {
        return LegacyImageResize.resizeUint8(input, inputWidth, inputHeight, inputStride,
                output, outputWidth, outputHeight, outputStride, channels);
    }
}
