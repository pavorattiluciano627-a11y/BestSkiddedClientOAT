package dev.echo.render.texture;

/** Texture metadata retains the original values, including the five-byte depth/stencil entry. */
public enum TextureFormat {
    Depth32Float(4, true, false),
    Depth32FloatStencil8(5, true, true),
    Depth24UnormStencil8(4, true, true),
    Depth24UnormX8(4, true, false),
    BGRA8Unorm(4, false, false),
    RGBA8Unorm(4, false, false),
    RGBA32Float(16, false, false),
    RGBA16Uint(8, false, false);

    public final int bytesPerPixel;
    public final boolean depth;
    public final boolean stencil;

    TextureFormat(int bytesPerPixel, boolean depth, boolean stencil) {
        this.bytesPerPixel = bytesPerPixel;
        this.depth = depth;
        this.stencil = stencil;
    }
}
