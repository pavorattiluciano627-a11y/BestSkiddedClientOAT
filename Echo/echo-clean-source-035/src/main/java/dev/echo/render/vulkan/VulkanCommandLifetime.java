package dev.echo.render.vulkan;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

/**
 * Vulkan transfer lifetime operations. Queue/device handles are borrowed; the
 * pool and command buffers are owned by their allocating TransferCommandPool.
 * Waiting, freeing a buffer, and destroying its pool are distinct operations.
 */
public final class VulkanCommandLifetime {
    private VulkanCommandLifetime() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /** Preserves the original operation's intentionally unchecked VkResult. */
    public static void waitIdle(VkQueue queue) {
        VK10.vkQueueWaitIdle(queue);
    }

    /** The owner must ensure that this command buffer is no longer pending. */
    public static void freeCommandBuffer(VkDevice device, long commandPool, VkCommandBuffer commands) {
        VK10.vkFreeCommandBuffers(device, commandPool, commands);
    }

    /** One-time pool disposal; does not wait, destroy the device, or own the queue. */
    public static void destroyCommandPool(VkDevice device, long commandPool) {
        VK10.vkDestroyCommandPool(device, commandPool, null);
    }
}
