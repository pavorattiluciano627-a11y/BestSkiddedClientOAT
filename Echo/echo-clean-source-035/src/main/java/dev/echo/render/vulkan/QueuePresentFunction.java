package dev.echo.render.vulkan;

import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;

/** Injectable queue-presentation operation, returning the Vulkan result code. */
@FunctionalInterface
public interface QueuePresentFunction {
    int present(VkQueue queue, VkPresentInfoKHR presentInfo);
}
