package dev.echo.compat.lwjgl;

import dev.echo.render.nativeapi.NativeLibraries;
import java.io.IOException;

/** The STB resize v0.97 operation used by Minecraft 1.21.11's NativeImage. */
public final class LegacyImageResize {
    static {
        try {
            NativeLibraries.load("echo_stb_legacy");
        } catch (IOException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private LegacyImageResize() {}

    /**
     * Forwards the original raw-pointer operation unchanged. The caller owns both
     * buffers and their bounds, exactly as for LWJGL's former unsafe entry point.
     * STB1 selects Catmull-Rom upsampling, Mitchell downsampling, clamped edges,
     * linear unsigned bytes, and independent channels (including alpha).
     *
     * @return STB's unmodified integer success/failure result
     */
    public static native int resizeUint8(long input, int inputWidth, int inputHeight,
            int inputStride, long output, int outputWidth, int outputHeight,
            int outputStride, int channels);
}
