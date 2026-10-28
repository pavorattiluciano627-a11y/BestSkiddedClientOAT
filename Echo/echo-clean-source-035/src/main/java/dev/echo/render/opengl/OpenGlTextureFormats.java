package dev.echo.render.opengl;

import dev.echo.render.texture.TextureFormat;

/** Exact GL format conversions, including the original BGRA/RGBA internal-format alias. */
public final class OpenGlTextureFormats {
    private OpenGlTextureFormats() {}

    public static int internalFormat(TextureFormat format) {
        return switch (format) {
            case Depth32Float -> 36012;
            case Depth32FloatStencil8 -> 36013;
            case Depth24UnormStencil8 -> 35056;
            case Depth24UnormX8 -> 33190;
            case BGRA8Unorm, RGBA8Unorm -> 32856;
            case RGBA32Float -> 34836;
            case RGBA16Uint -> 36214;
        };
    }

    public static TextureFormat fromInternalFormat(int internalFormat) {
        return switch (internalFormat) {
            case 32856 -> TextureFormat.RGBA8Unorm;
            case 33190, 33191 -> TextureFormat.Depth24UnormX8;
            case 34836 -> TextureFormat.RGBA32Float;
            case 35056 -> TextureFormat.Depth24UnormStencil8;
            case 36012 -> TextureFormat.Depth32Float;
            case 36013 -> TextureFormat.Depth32FloatStencil8;
            case 36214 -> TextureFormat.RGBA16Uint;
            default -> throw new IllegalArgumentException("Unsupported GL internal format: " + internalFormat);
        };
    }

    public static int pixelFormat(TextureFormat format) {
        return switch (format) {
            case Depth32Float, Depth24UnormX8 -> 6402;
            case Depth32FloatStencil8, Depth24UnormStencil8 -> 34041;
            case BGRA8Unorm -> 32993;
            case RGBA8Unorm, RGBA32Float -> 6408;
            case RGBA16Uint -> 36249;
        };
    }

    public static int pixelType(TextureFormat format) {
        return switch (format) {
            case Depth32Float, RGBA32Float -> 5126;
            case Depth32FloatStencil8 -> 36269;
            case Depth24UnormStencil8 -> 34042;
            case Depth24UnormX8 -> 5125;
            case BGRA8Unorm, RGBA8Unorm -> 5121;
            case RGBA16Uint -> 5123;
        };
    }
}
