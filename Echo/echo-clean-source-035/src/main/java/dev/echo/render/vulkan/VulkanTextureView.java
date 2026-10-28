package dev.echo.render.vulkan;

import dev.echo.render.texture.TextureLayout;
import dev.echo.render.texture.TextureView;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

/** Owns a created VkImageView, but borrows image views passed to the import constructor. */
public class VulkanTextureView extends TextureView {
    public final long imageView;
    public final long image;
    private final boolean ownsImageView;

    public VulkanTextureView(VulkanTexture texture, int baseMipLevel, int mipCount) {
        super(texture, baseMipLevel, mipCount);
        image = texture.image;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo info = VkImageViewCreateInfo.calloc(stack).sType$Default().image(image)
                    .viewType(texture.depth > 1 ? VK10.VK_IMAGE_VIEW_TYPE_3D : VK10.VK_IMAGE_VIEW_TYPE_2D)
                    .format(texture.vkFormat);
            info.subresourceRange().aspectMask(VulkanTexture.aspectMask(texture.vkFormat))
                    .baseMipLevel(baseMipLevel).levelCount(mipCount).baseArrayLayer(0).layerCount(1);
            LongBuffer result = stack.mallocLong(1);
            VulkanChecks.checkSuccess(VK10.vkCreateImageView(texture.device.device, info, null, result),
                    "Failed to create image view");
            imageView = result.get(0);
        }
        ownsImageView = true;
    }

    public VulkanTextureView(VulkanTexture texture, int baseMipLevel, int mipCount, long imageView) {
        super(texture, baseMipLevel, mipCount);
        image = texture.image;
        this.imageView = imageView;
        ownsImageView = false;
        keepGeneralLayout = true;
        layout = TextureLayout.GENERAL;
    }

    @Override
    public void destroy() {
        if (ownsImageView) VK10.vkDestroyImageView(((VulkanTexture) texture).device.device, imageView, null);
    }
}
