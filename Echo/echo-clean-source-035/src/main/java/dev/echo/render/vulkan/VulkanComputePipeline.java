package dev.echo.render.vulkan;

import dev.echo.render.pipeline.ComputePipeline;
import dev.echo.render.pipeline.ComputePipelineInfo;
import dev.echo.render.shader.ShaderResourceLayout;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkComputePipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;

/** Vulkan compute pipeline; owns its shader, descriptor-set layout and pipeline layout. */
class VulkanComputePipeline extends ComputePipeline {
    private final long shaderModule;
    private final long descriptorSetLayout;
    public final long pipelineLayout;
    private final long pipeline;
    private final ShaderResourceLayout resourceLayout;
    private final VulkanDevice device;

    public VulkanComputePipeline(VulkanDevice device, ComputePipelineInfo info) {
        super(info);
        this.device = device;
        resourceLayout = ShaderResourceLayout.reflect(info.module);
        ByteBuffer spirv = VulkanShaderCompiler.compileSpirv(info.module);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            shaderModule = VulkanShaderCompiler.createShaderModule(device.device, stack, spirv);
            descriptorSetLayout = VulkanShaderCompiler.createDescriptorSetLayout(
                    device.device, stack, resourceLayout, 32);
            pipelineLayout = VulkanShaderCompiler.createPipelineLayout(device.device, stack,
                    descriptorSetLayout, info.pushConstantBytes, 32);
            pipeline = createPipeline(stack);
        }
        // Original frees after success, not in a finally block.
        MemoryUtil.memFree(spirv);
    }

    public int bindingKind(int binding) {
        return VulkanShaderCompiler.vkDescriptorType(resourceLayout.bindingKinds()[binding]);
    }

    public void bind(VkCommandBuffer commands) { VulkanPipelineOperations.bindCompute(commands, pipeline); }

    public void dispatch(VkCommandBuffer commands, int x, int y, int z) {
        VulkanPipelineOperations.dispatchCompute(commands, x, y, z);
    }

    @Override public void destroy() {
        VulkanPipelineOperations.deleteComputePipeline(device.host::device, pipeline,
                pipelineLayout, descriptorSetLayout, shaderModule);
    }

    private long createPipeline(MemoryStack stack) {
        ByteBuffer entryPoint = stack.UTF8("cs_main");
        VkPipelineShaderStageCreateInfo stage = VkPipelineShaderStageCreateInfo.calloc(stack)
                .sType$Default().stage(32).module(shaderModule).pName(entryPoint);
        VkComputePipelineCreateInfo.Buffer createInfo = VkComputePipelineCreateInfo.calloc(1, stack)
                .sType$Default().stage(stage).layout(pipelineLayout)
                .basePipelineHandle(0L).basePipelineIndex(-1);
        LongBuffer result = stack.mallocLong(1);
        VulkanChecks.checkSuccess(VK10.vkCreateComputePipelines(device.host.device(), 0L,
                createInfo, null, result), "Failed to create compute pipeline");
        return result.get(0);
    }
}
