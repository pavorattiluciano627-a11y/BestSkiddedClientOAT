package dev.echo.render.vulkan;

import dev.echo.render.command.CommandEncoder;
import dev.echo.render.queue.CommandQueue;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import static org.lwjgl.vulkan.VK10.*;
import static dev.echo.render.vulkan.VulkanChecks.checkSuccess;

/** Owns a resettable command pool, one primary command buffer, and a submission fence. */
public class VulkanQueue extends CommandQueue {
    private final VulkanDevice device;
    public final VkQueue queue;
    private final int capabilities;
    private final long commandPool;
    private final long fence;
    private final VkCommandBuffer commandBuffer;
    private boolean submitted;

    VulkanQueue(VulkanDevice device, VkQueue queue, int familyIndex, int capabilities) {
        this.device = device;
        this.queue = queue;
        this.capabilities = capabilities;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack).sType$Default()
                    .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT).queueFamilyIndex(familyIndex);
            LongBuffer pool = stack.mallocLong(1);
            checkSuccess(vkCreateCommandPool(device.device, poolInfo, null, pool), "Failed to create command pool");
            commandPool = pool.get(0);
            VkCommandBufferAllocateInfo allocate = VkCommandBufferAllocateInfo.calloc(stack).sType$Default()
                    .commandPool(commandPool).level(VK_COMMAND_BUFFER_LEVEL_PRIMARY).commandBufferCount(1);
            PointerBuffer command = stack.mallocPointer(1);
            checkSuccess(vkAllocateCommandBuffers(device.device, allocate, command), "Failed to allocate command buffer");
            commandBuffer = new VkCommandBuffer(command.get(0), device.device);
            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack).sType$Default();
            LongBuffer result = stack.mallocLong(1);
            checkSuccess(vkCreateFence(device.device, fenceInfo, null, result), "Failed to create fence");
            fence = result.get(0);
        }
    }

    @Override public int capabilities() { return capabilities; }

    @Override
    public CommandEncoder beginCommands() {
        if (submitted) {
            vkWaitForFences(device.device, fence, true, Long.MAX_VALUE);
            vkResetFences(device.device, fence);
            submitted = false;
        }
        vkResetCommandBuffer(commandBuffer, 0);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo begin = VkCommandBufferBeginInfo.calloc(stack).sType$Default()
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            checkSuccess(vkBeginCommandBuffer(commandBuffer, begin), "Failed to begin command buffer");
        }
        return new VulkanCommandEncoder(device, commandBuffer);
    }

    @Override public void submit(CommandEncoder commands) { submit(commands, 0, 0); }

    void submit(CommandEncoder commands, long waitSemaphore, long signalSemaphore) {
        VkCommandBuffer command = ((VulkanCommandEncoder) commands).commandBuffer();
        device.barriers.flush(command);
        checkSuccess(vkEndCommandBuffer(command), "Failed to end command buffer");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSubmitInfo info = VkSubmitInfo.calloc(stack).sType$Default().pCommandBuffers(stack.pointers(command));
            if (waitSemaphore != 0) {
                info.waitSemaphoreCount(1).pWaitSemaphores(stack.longs(waitSemaphore))
                        .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            }
            if (signalSemaphore != 0) info.pSignalSemaphores(stack.longs(signalSemaphore));
            checkSuccess(vkQueueSubmit(queue, info, fence), "Failed to submit command buffer");
            submitted = true;
        }
    }

    @Override public void waitIdle() { checkSuccess(vkQueueWaitIdle(queue), "Failed to wait on queue"); }

    @Override
    public void destroy() {
        waitIdle();
        vkDestroyFence(device.device, fence, null);
        vkDestroyCommandPool(device.device, commandPool, null);
    }
}
