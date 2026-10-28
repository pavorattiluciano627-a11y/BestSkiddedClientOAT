package dev.echo.render.vulkan;

import org.lwjgl.vulkan.VkQueue;

/** A Vulkan queue and the queue-family index from which it was obtained. */
public record VulkanQueueFamily(VkQueue queue, int familyIndex) {
}
