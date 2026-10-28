package dev.echo.render.vulkan;

import dev.echo.render.texture.TextureFormat;

/** The eight supported VkFormat values; unsupported reverse conversions remain errors. */
public final class VulkanTextureFormats {
    private VulkanTextureFormats() {}

    public static int vkFormat(TextureFormat format) {
        return switch (format) {
            case Depth32Float -> 126;
            case Depth32FloatStencil8 -> 130;
            case Depth24UnormStencil8 -> 129;
            case Depth24UnormX8 -> 125;
            case BGRA8Unorm -> 44;
            case RGBA8Unorm -> 37;
            case RGBA32Float -> 109;
            case RGBA16Uint -> 95;
        };
    }

    public static TextureFormat fromVkFormat(int format) {
        return switch (format) {
            case 37 -> TextureFormat.RGBA8Unorm;
            case 44 -> TextureFormat.BGRA8Unorm;
            case 95 -> TextureFormat.RGBA16Uint;
            case 109 -> TextureFormat.RGBA32Float;
            case 125 -> TextureFormat.Depth24UnormX8;
            case 126 -> TextureFormat.Depth32Float;
            case 129 -> TextureFormat.Depth24UnormStencil8;
            case 130 -> TextureFormat.Depth32FloatStencil8;
            default -> throw new IllegalArgumentException("Unsupported VkFormat: " + format);
        };
    }
}
