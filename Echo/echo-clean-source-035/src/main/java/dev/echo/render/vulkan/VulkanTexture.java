package dev.echo.render.vulkan;

import dev.echo.render.texture.Texture;
import dev.echo.render.texture.TextureFormat;
import dev.echo.render.texture.TextureView;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkImageCreateInfo;

/** VMA image owner, or a borrowed image/view pair supplied by the host or swapchain. */
public class VulkanTexture extends Texture {
    public final VulkanDevice device;
    public final long image;
    public final int vkFormat;
    private final long allocation;
    private final boolean ownsImage;

    public VulkanTexture(VulkanDevice device, int width, int height, int depth, TextureFormat format,
                         int usage, int mipCount) {
        super(width, height, depth, format, usage, mipCount);
        this.device = device;
        vkFormat = VulkanTextureFormats.vkFormat(format);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack).sType$Default()
                    .imageType(depth > 1 ? VK10.VK_IMAGE_TYPE_3D : VK10.VK_IMAGE_TYPE_2D)
                    .format(vkFormat).mipLevels(mipCount).arrayLayers(1).samples(VK10.VK_SAMPLE_COUNT_1_BIT)
                    .tiling(VK10.VK_IMAGE_TILING_OPTIMAL).usage(vkImageUsage(usage, format))
                    .sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE).initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.extent().width(width).height(height).depth(depth);
            IntBuffer families = (usage & 4) != 0 ? distinctQueueFamilies(stack, device) : null;
            if (families != null) imageInfo.sharingMode(VK10.VK_SHARING_MODE_CONCURRENT).pQueueFamilyIndices(families);
            VmaAllocationCreateInfo allocationInfo = VmaAllocationCreateInfo.calloc(stack).usage(7);
            LongBuffer imageResult = stack.mallocLong(1);
            PointerBuffer allocationResult = stack.mallocPointer(1);
            VulkanChecks.checkSuccess(Vma.vmaCreateImage(device.allocator, imageInfo, allocationInfo,
                    imageResult, allocationResult, null), "Failed to create image");
            image = imageResult.get(0);
            allocation = allocationResult.get(0);
        }
        ownsImage = true;
        views = new VulkanTextureView[mipCount];
        for (int mip = 0; mip < mipCount; mip++) views[mip] = new VulkanTextureView(this, mip, 1);
    }

    public VulkanTexture(VulkanDevice device, long image, long imageView, int width, int height,
                         TextureFormat format, int vkFormat) {
        super(width, height, format, 1, 1);
        this.device = device;
        this.vkFormat = vkFormat;
        this.image = image;
        allocation = 0;
        ownsImage = false;
        views = new VulkanTextureView[]{new VulkanTextureView(this, 0, 1, imageView)};
    }

    private static IntBuffer distinctQueueFamilies(MemoryStack stack, VulkanDevice device) {
        var families = device.host.queueFamilies();
        IntBuffer indices = stack.mallocInt(families.length);
        for (var family : families) {
            boolean alreadyAdded = false;
            for (int index = 0; index < indices.position(); index++) {
                alreadyAdded |= indices.get(index) == family.familyIndex();
            }
            if (!alreadyAdded) indices.put(family.familyIndex());
        }
        return indices.position() < 2 ? null : indices.flip();
    }

    private static int vkImageUsage(int usage, TextureFormat format) {
        int flags = 0;
        if ((usage & 1) != 0) flags |= VK10.VK_IMAGE_USAGE_SAMPLED_BIT;
        if ((usage & 2) != 0) flags |= VK10.VK_IMAGE_USAGE_STORAGE_BIT;
        if ((usage & 4) != 0) flags |= VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT;
        if ((usage & 8) != 0) {
            flags |= format.depth ? VK10.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT : VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
        }
        return flags;
    }

    public static int aspectMask(int vkFormat) {
        return switch (vkFormat) {
            case 124, 125, 126 -> VK10.VK_IMAGE_ASPECT_DEPTH_BIT;
            case 128, 129, 130 -> VK10.VK_IMAGE_ASPECT_DEPTH_BIT | VK10.VK_IMAGE_ASPECT_STENCIL_BIT;
            case 127 -> VK10.VK_IMAGE_ASPECT_STENCIL_BIT;
            default -> VK10.VK_IMAGE_ASPECT_COLOR_BIT;
        };
    }

    @Override
    public void destroy() {
        for (TextureView view : views) view.destroy();
        if (ownsImage) Vma.vmaDestroyImage(device.allocator, image, allocation);
    }
}
