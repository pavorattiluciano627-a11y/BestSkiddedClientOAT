package dev.echo.render.vulkan;

import java.util.function.Supplier;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;

/** Direct draw, compute and disposal operations on existing Vulkan resources. */
final class VulkanPipelineOperations {
    private VulkanPipelineOperations() {
    }

    static void bindCompute(VkCommandBuffer commands, long pipeline) {
        VK10.vkCmdBindPipeline(commands, 1, pipeline);
    }

    static void dispatchCompute(VkCommandBuffer commands, int x, int y, int z) {
        VK10.vkCmdDispatch(commands, x, y, z);
    }

    static void pushComputeConstants(VkCommandBuffer commands, long layout, long address, int bytes) {
        VK10.nvkCmdPushConstants(commands, layout, 32, 0, bytes, address);
    }

    static void draw(VkCommandBuffer commands, int count, int instances, int first, int baseInstance) {
        VK10.vkCmdDraw(commands, count, instances, first, baseInstance);
    }

    static void drawIndexed(VkCommandBuffer commands, int count, int instances, int firstIndex) {
        VK10.vkCmdDrawIndexed(commands, count, instances, firstIndex, 0, 0);
    }

    static void drawIndirect(VkCommandBuffer commands, long buffer, long offset, int count, int stride) {
        VK10.vkCmdDrawIndirect(commands, buffer, offset, count, stride);
    }

    static void deleteComputePipeline(Supplier<VkDevice> device, long pipeline, long layout,
                                      long descriptorSetLayout, long shaderModule) {
        // Keep the four separate host lookups; a cached VkDevice changes the contract.
        VK10.vkDestroyPipeline(device.get(), pipeline, null);
        VK10.vkDestroyPipelineLayout(device.get(), layout, null);
        VK10.vkDestroyDescriptorSetLayout(device.get(), descriptorSetLayout, null);
        VK10.vkDestroyShaderModule(device.get(), shaderModule, null);
    }
}
