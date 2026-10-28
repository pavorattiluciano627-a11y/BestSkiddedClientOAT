package dev.echo.render.vulkan;

import dev.echo.render.command.CommandEncoder;
import dev.echo.render.queue.CommandQueue;
import dev.echo.render.surface.PresentMode;
import dev.echo.render.surface.Surface;
import dev.echo.render.surface.SurfaceInfo;
import dev.echo.render.texture.TextureFormat;
import dev.echo.render.texture.TextureView;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import org.slf4j.LoggerFactory;

/** Swapchain owner; acquisition semaphores rotate independently of image-indexed present semaphores. */
public class VulkanSurface extends Surface {
    public static QueuePresentFunction queuePresentFunction;
    private final VulkanDevice device;
    private final TextureFormat format;
    public final long surface;
    private long swapchain;
    private VulkanTexture[] images = new VulkanTexture[0];
    private long[] acquireSemaphores = new long[0];
    private long[] presentSemaphores = new long[0];
    private int width;
    private int height;
    private int acquiredImageIndex = -1;
    private int acquireSemaphoreIndex;
    private boolean resizeRequired;

    public VulkanSurface(VulkanDevice device, SurfaceInfo info) {
        this.device = device;
        format = info.format;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            surface = info.windowSystem.createVulkanSurface(device.device.getPhysicalDevice().getInstance(), info.windowHandle);
            IntBuffer supported = stack.mallocInt(1);
            VulkanChecks.checkSuccess(KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(device.device.getPhysicalDevice(),
                    device.host.graphicsQueueFamily().familyIndex(), surface, supported), "Failed to query surface present support");
            if (supported.get(0) != VK10.VK_TRUE) {
                throw new IllegalStateException("Graphics queue family cannot present to this surface");
            }
        }
    }

    @Override
    public void resize(int width, int height, PresentMode presentMode) {
        VK10.vkDeviceWaitIdle(device.device);
        long oldSwapchain = swapchain;
        destroySwapchainAttachments();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDevice physicalDevice = device.device.getPhysicalDevice();
            VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.calloc(stack);
            VulkanChecks.checkSuccess(KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice,
                    surface, capabilities), "Failed to query surface capabilities");
            this.width = Math.clamp((long) width, capabilities.minImageExtent().width(), capabilities.maxImageExtent().width());
            this.height = Math.clamp((long) height, capabilities.minImageExtent().height(), capabilities.maxImageExtent().height());
            int imageCount = capabilities.minImageCount() + 1;
            if (capabilities.maxImageCount() > 0 && imageCount > capabilities.maxImageCount()) {
                imageCount = capabilities.maxImageCount();
            }
            VkSwapchainCreateInfoKHR info = VkSwapchainCreateInfoKHR.calloc(stack).sType$Default()
                    .surface(surface).minImageCount(imageCount).imageFormat(selectSurfaceFormat(stack, physicalDevice))
                    .imageColorSpace(KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR).imageArrayLayers(1)
                    .imageUsage(VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT)
                    .imageSharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE).preTransform(selectPreTransform(capabilities))
                    .compositeAlpha(selectCompositeAlpha(capabilities)).presentMode(selectPresentMode(stack, physicalDevice, presentMode))
                    .clipped(true).oldSwapchain(oldSwapchain);
            info.imageExtent().width(this.width).height(this.height);
            LongBuffer result = stack.mallocLong(1);
            VulkanChecks.checkSuccess(KHRSwapchain.vkCreateSwapchainKHR(device.device, info, null, result), "Failed to create swapchain");
            swapchain = result.get(0);
        }
        if (oldSwapchain != 0) KHRSwapchain.vkDestroySwapchainKHR(device.device, oldSwapchain, null);
        createSwapchainAttachments();
        resizeRequired = false;
        acquiredImageIndex = -1;
        acquireSemaphoreIndex = 0;
    }

    @Override
    public boolean needsResize() { return resizeRequired; }

    @Override
    public TextureView acquireTexture() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long acquireSemaphore = acquireSemaphores[acquireSemaphoreIndex];
            IntBuffer index = stack.mallocInt(1);
            int result = KHRSwapchain.vkAcquireNextImageKHR(device.device, swapchain, Long.MAX_VALUE, acquireSemaphore, 0, index);
            if (result == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
                resizeRequired = true;
                return null;
            }
            if (result != KHRSwapchain.VK_SUBOPTIMAL_KHR) {
                VulkanChecks.checkSuccess(result, "Failed to acquire swapchain image");
            }
            acquiredImageIndex = index.get(0);
            return images[acquiredImageIndex].views[0];
        }
    }

    @Override
    public void present(CommandQueue queue, CommandEncoder commands) {
        VulkanQueue vulkanQueue = (VulkanQueue) queue;
        vulkanQueue.submit(commands, acquireSemaphores[acquireSemaphoreIndex], presentSemaphores[acquiredImageIndex]);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPresentInfoKHR info = VkPresentInfoKHR.calloc(stack).sType$Default()
                    .pWaitSemaphores(stack.longs(presentSemaphores[acquiredImageIndex])).swapchainCount(1)
                    .pSwapchains(stack.longs(swapchain)).pImageIndices(stack.ints(acquiredImageIndex));
            int result = queuePresentFunction == null
                    ? KHRSwapchain.vkQueuePresentKHR(vulkanQueue.queue, info) : queuePresentFunction.present(vulkanQueue.queue, info);
            if (result == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
                resizeRequired = true;
            } else if (result != KHRSwapchain.VK_SUBOPTIMAL_KHR) {
                VulkanChecks.checkSuccess(result, "Failed to present swapchain image");
            }
        }
        acquireSemaphoreIndex = (acquireSemaphoreIndex + 1) % acquireSemaphores.length;
        acquiredImageIndex = -1;
    }

    @Override
    public void destroy() {
        VK10.vkDeviceWaitIdle(device.device);
        destroySwapchainAttachments();
        if (swapchain != 0) {
            KHRSwapchain.vkDestroySwapchainKHR(device.device, swapchain, null);
            swapchain = 0;
        }
        KHRSurface.vkDestroySurfaceKHR(device.device.getPhysicalDevice().getInstance(), surface, null);
    }

    private void createSwapchainAttachments() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            VulkanChecks.checkSuccess(KHRSwapchain.vkGetSwapchainImagesKHR(device.device, swapchain, count, null), "Failed to count swapchain images");
            LongBuffer handles = stack.mallocLong(count.get(0));
            VulkanChecks.checkSuccess(KHRSwapchain.vkGetSwapchainImagesKHR(device.device, swapchain, count, handles), "Failed to get swapchain images");
            int imageCount = count.get(0);
            images = new VulkanTexture[imageCount];
            acquireSemaphores = new long[imageCount];
            presentSemaphores = new long[imageCount];
            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack).sType$Default();
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack).sType$Default()
                    .viewType(VK10.VK_IMAGE_VIEW_TYPE_2D).format(VulkanTextureFormats.vkFormat(format));
            viewInfo.subresourceRange().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1);
            LongBuffer result = stack.mallocLong(1);
            for (int index = 0; index < imageCount; index++) {
                long image = handles.get(index);
                viewInfo.image(image);
                VulkanChecks.checkSuccess(VK10.vkCreateImageView(device.device, viewInfo, null, result), "Failed to create swapchain image view");
                images[index] = new VulkanTexture(device, image, result.get(0), width, height, format, VulkanTextureFormats.vkFormat(format));
                VulkanChecks.checkSuccess(VK10.vkCreateSemaphore(device.device, semaphoreInfo, null, result), "Failed to create acquire semaphore");
                acquireSemaphores[index] = result.get(0);
                VulkanChecks.checkSuccess(VK10.vkCreateSemaphore(device.device, semaphoreInfo, null, result), "Failed to create present semaphore");
                presentSemaphores[index] = result.get(0);
            }
        }
    }

    private void destroySwapchainAttachments() {
        for (VulkanTexture image : images) {
            // Imported texture views are borrowed; this surface owns and destroys them directly.
            VK10.vkDestroyImageView(device.device, ((VulkanTextureView) image.views[0]).imageView, null);
        }
        for (long semaphore : acquireSemaphores) VK10.vkDestroySemaphore(device.device, semaphore, null);
        for (long semaphore : presentSemaphores) VK10.vkDestroySemaphore(device.device, semaphore, null);
        images = new VulkanTexture[0];
        acquireSemaphores = new long[0];
        presentSemaphores = new long[0];
    }

    private int selectSurfaceFormat(MemoryStack stack, VkPhysicalDevice physicalDevice) {
        int preferred = VulkanTextureFormats.vkFormat(format);
        IntBuffer count = stack.mallocInt(1);
        KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, count, null);
        VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.calloc(count.get(0), stack);
        KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, count, formats);
        for (int index = 0; index < formats.capacity(); index++) {
            if (formats.get(index).format() == preferred) return preferred;
        }
        return formats.get(0).format();
    }

    private int selectPresentMode(MemoryStack stack, VkPhysicalDevice physicalDevice, PresentMode mode) {
        int preferred = vkPresentMode(mode);
        IntBuffer count = stack.mallocInt(1);
        KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, count, null);
        IntBuffer modes = stack.mallocInt(count.get(0));
        KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, count, modes);
        for (int index = 0; index < modes.capacity(); index++) {
            if (modes.get(index) == preferred) return preferred;
        }
        return KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
    }

    private static int selectCompositeAlpha(VkSurfaceCapabilitiesKHR capabilities) {
        if ((capabilities.supportedCompositeAlpha() & KHRSurface.VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR) != 0) {
            return KHRSurface.VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR;
        }
        if ((capabilities.supportedCompositeAlpha() & KHRSurface.VK_COMPOSITE_ALPHA_POST_MULTIPLIED_BIT_KHR) != 0) {
            return KHRSurface.VK_COMPOSITE_ALPHA_POST_MULTIPLIED_BIT_KHR;
        }
        LoggerFactory.getLogger("chimera").warn("Surface does not support alpha compositing; overlay will be opaque");
        return KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
    }

    private static int selectPreTransform(VkSurfaceCapabilitiesKHR capabilities) {
        return (capabilities.supportedTransforms() & KHRSurface.VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR) != 0
                ? KHRSurface.VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR : capabilities.currentTransform();
    }

    private static int vkPresentMode(PresentMode mode) {
        return switch (mode) {
            case IMMEDIATE -> KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR;
            case MAILBOX -> KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
            case FIFO -> KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
            case FIFO_RELAXED -> KHRSurface.VK_PRESENT_MODE_FIFO_RELAXED_KHR;
        };
    }
}
