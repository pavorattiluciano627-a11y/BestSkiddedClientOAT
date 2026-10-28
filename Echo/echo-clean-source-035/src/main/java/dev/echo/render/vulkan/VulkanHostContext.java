package dev.echo.render.vulkan;

import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;

/**
 * Host-owned Vulkan state borrowed by the renderer.
 *
 * <p>Analysis candidate: all eleven original abstract declarations are retained.
 * Two declarations keep their original identifiers until their semantic roles
 * can be established. This interface supplies no host implementation and does
 * not authorize production promotion of its conditional dependencies.
 */
public interface VulkanHostContext {
    /** The host's transfer command pool; ownership remains with the host. */
    TransferCommandPool transferPool();

    /** The existing logical device used by resource creation and disposal. */
    VkDevice device();

    /** The command buffer in the host's current recording scope. */
    VkCommandBuffer currentCommandBuffer();

    /** The host's Vulkan Memory Allocator handle. */
    long allocator();

    /** Index of the frame whose deferred resources may be retired. */
    int frameIndex();

    /** Number of frame-retirement queues allocated by the renderer. */
    int framesInFlight();

    /** Queue and family used by the surface's presentation-support check. */
    VulkanQueueFamily graphicsQueueFamily();

    /** Created queue/family pairs available to queue selection. */
    VulkanQueueFamily[] queueFamilies();

    /** Original float-valued host declaration; semantic name remains unresolved. */
    float registerRecord();

    /** Original int-valued host declaration; semantic name remains unresolved. */
    int writeFeature();

    /** The host depth VkFormat, converted by the renderer's format table. */
    int depthFormat();
}
