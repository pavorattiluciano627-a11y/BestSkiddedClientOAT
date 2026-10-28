package dev.echo.render.vulkan;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkSubmitInfo;

/**
 * Conditional reconstruction awaiting per-method guard/key proofs. Owns its pool;
 * queue and device are borrowed. begin/submit do not wait or free commands.
 */
public class TransferCommandPool {
    private final VkQueue queue;
    private final VkDevice device;
    private final long commandPool;

    public TransferCommandPool(VkDevice device) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int family = findTransferQueueFamily(stack);
            PointerBuffer outputQueue = stack.mallocPointer(1);
            VK10.vkGetDeviceQueue(device, family, 0, outputQueue);
            queue = new VkQueue(outputQueue.get(0), device);
            VkCommandPoolCreateInfo info = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .queueFamilyIndex(family).flags(VK10.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);
            LongBuffer outputPool = stack.mallocLong(1);
            VulkanChecks.checkSuccess(VK10.vkCreateCommandPool(device, info, null, outputPool),
                    "Failed to create transfer command pool");
            commandPool = outputPool.get(0);
        }
    }

    private int findTransferQueueFamily(MemoryStack stack) {
        IntBuffer count = stack.mallocInt(1);
        VK10.vkGetPhysicalDeviceQueueFamilyProperties(device.getPhysicalDevice(), count, null);
        VkQueueFamilyProperties.Buffer families = VkQueueFamilyProperties.malloc(count.get(0), stack);
        VK10.vkGetPhysicalDeviceQueueFamilyProperties(device.getPhysicalDevice(), count, families);
        for (int index = 0; index < families.capacity(); index++) {
            int flags = families.get(index).queueFlags();
            if ((flags & VK10.VK_QUEUE_TRANSFER_BIT) != 0 && (flags & VK10.VK_QUEUE_GRAPHICS_BIT) == 0) {
                return index;
            }
        }
        for (int index = 0; index < families.capacity(); index++) {
            if ((families.get(index).queueFlags() & VK10.VK_QUEUE_TRANSFER_BIT) != 0) {
                return index;
            }
        }
        throw new IllegalStateException("No transfer queue family found");
    }

    public TransferCommandPool(VkDevice device, VulkanQueueFamily family) {
        this.device = device;
        queue = family.queue();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo info = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .queueFamilyIndex(family.familyIndex()).flags(VK10.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);
            LongBuffer outputPool = stack.mallocLong(1);
            VulkanChecks.checkSuccess(VK10.vkCreateCommandPool(device, info, null, outputPool),
                    "Failed to create transfer command pool");
            commandPool = outputPool.get(0);
        }
    }

    public VkCommandBuffer begin() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocation = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool).level(VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY).commandBufferCount(1);
            PointerBuffer output = stack.mallocPointer(1);
            VulkanChecks.checkSuccess(VK10.vkAllocateCommandBuffers(device, allocation, output),
                    "Failed to allocate transfer command buffer");
            VkCommandBuffer commands = new VkCommandBuffer(output.get(0), device);
            VkCommandBufferBeginInfo info = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            VulkanChecks.checkSuccess(VK10.vkBeginCommandBuffer(commands, info),
                    "Failed to begin transfer command buffer");
            return commands;
        }
    }

    public void submit(VkCommandBuffer commands) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VulkanChecks.checkSuccess(VK10.vkEndCommandBuffer(commands), "Failed to end transfer command buffer");
            VkSubmitInfo info = VkSubmitInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(commands));
            VulkanChecks.checkSuccess(VK10.vkQueueSubmit(queue, info, VK10.VK_NULL_HANDLE),
                    "Failed to submit transfer command buffer");
        }
    }

    public void waitIdle() {
        VK10.vkQueueWaitIdle(queue);
    }

    public void freeCommandBuffer(VkCommandBuffer commands) {
        VK10.vkFreeCommandBuffers(device, commandPool, commands);
    }

    public void destroy() {
        VK10.vkDestroyCommandPool(device, commandPool, null);
    }
}
