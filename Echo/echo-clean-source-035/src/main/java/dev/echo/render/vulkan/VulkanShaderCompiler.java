package dev.echo.render.vulkan;

import dev.echo.render.shader.ResourceBindingKind;
import dev.echo.render.shader.ShaderResourceLayout;
import dev.echo.render.shader.naga.Module;
import dev.echo.render.shader.naga.ModuleInfo;
import dev.echo.render.shader.naga.Validator;
import dev.echo.render.shader.spirv.SpirvOptions;
import dev.echo.render.shader.spirv.SpirvWriter;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRPushDescriptor;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

/**
 * Conditional reconstruction: all removed key/guard paths and Naga signatures
 * require foundation reconciliation. Returned native allocations belong to callers.
 */
final class VulkanShaderCompiler {
    private VulkanShaderCompiler() {}

    public static ByteBuffer compileSpirv(Module module) {
        byte[] bytes;
        // Literal validation/capability flags are those of the original compiler.
        try (ModuleInfo info = new Validator(63, 68619L).validate(module)) {
            SpirvOptions options = new SpirvOptions();
            options.flags = 0;
            bytes = new SpirvWriter(options).write(module, info);
        }
        ByteBuffer code = MemoryUtil.memAlloc(bytes.length);
        code.put(bytes).flip();
        return code;
    }

    public static long createShaderModule(VkDevice device, MemoryStack stack, ByteBuffer code) {
        VkShaderModuleCreateInfo info = VkShaderModuleCreateInfo.calloc(stack).sType$Default().pCode(code);
        LongBuffer output = stack.mallocLong(1);
        VulkanChecks.checkSuccess(VK10.vkCreateShaderModule(device, info, null, output),
                "Failed to create shader module");
        return output.get(0);
    }

    static long createDescriptorSetLayout(VkDevice device, MemoryStack stack,
            ShaderResourceLayout layout, int stageFlags) {
        ResourceBindingKind[] kinds = layout.bindingKinds();
        int[] counts = layout.bindingCounts();
        int bindingCount = 0;
        for (ResourceBindingKind kind : kinds) {
            if (kind != null) {
                bindingCount++;
            }
        }
        VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(bindingCount, stack);
        int outputIndex = 0;
        for (int binding = 0; binding < kinds.length; binding++) {
            if (kinds[binding] != null) {
                bindings.get(outputIndex++).binding(binding).descriptorType(vkDescriptorType(kinds[binding]))
                        .descriptorCount(counts[binding]).stageFlags(stageFlags);
            }
        }
        VkDescriptorSetLayoutCreateInfo info = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default()
                .flags(KHRPushDescriptor.VK_DESCRIPTOR_SET_LAYOUT_CREATE_PUSH_DESCRIPTOR_BIT_KHR).pBindings(bindings);
        LongBuffer output = stack.mallocLong(1);
        VulkanChecks.checkSuccess(VK10.vkCreateDescriptorSetLayout(device, info, null, output),
                "Failed to create descriptor set layout");
        return output.get(0);
    }

    public static long createPipelineLayout(VkDevice device, MemoryStack stack,
            long descriptorSetLayout, int pushConstantBytes, int stageFlags) {
        VkPipelineLayoutCreateInfo info = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
                .pSetLayouts(stack.longs(descriptorSetLayout));
        if (pushConstantBytes > 0) {
            VkPushConstantRange.Buffer range = VkPushConstantRange.calloc(1, stack);
            range.get(0).stageFlags(stageFlags).offset(0).size(pushConstantBytes);
            info.pPushConstantRanges(range);
        }
        LongBuffer output = stack.mallocLong(1);
        VulkanChecks.checkSuccess(VK10.vkCreatePipelineLayout(device, info, null, output),
                "Failed to create pipeline layout");
        return output.get(0);
    }

    public static int vkDescriptorType(ResourceBindingKind kind) {
        return switch (kind) {
            case UNIFORM -> VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
            case STORAGE_BUFFER -> VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
            case SAMPLED_IMAGE, SAMPLED_IMAGE_ARRAY -> VK10.VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE;
            case SAMPLER -> VK10.VK_DESCRIPTOR_TYPE_SAMPLER;
            case STORAGE_IMAGE -> VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE;
        };
    }
}
